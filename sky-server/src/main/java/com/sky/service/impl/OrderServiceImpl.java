package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //Service 处理业务异常
        //判断各种异常情况

        //判断地址簿是否为空，为空无法配送
        AddressBook book = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(book == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
    //检查范围
        //市级名称+区级名称+详细地址
        checkOutOfRange(book.getCityName() + book.getDistrictName() + book.getDetail());

        //购物车为空，不进行下单操作
        //根据用户Id可以唯一确定某个用户的购物车
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(shoppingCart);
        if(shoppingCarts == null || shoppingCarts.size() == 0 ){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        //下单时间
        order.setOrderTime(LocalDateTime.now());
        //支付状态
        order.setPayStatus(Orders.UN_PAID);  //代表未支付
        //设置订单号
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        //订单状态
        order.setStatus(Orders.PENDING_PAYMENT);
        //手机号
        order.setPhone(book.getPhone());
        order.setUserId(userId);
        //收货人
        order.setConsignee(book.getConsignee());

        //向订单表插入数据
        orderMapper.insert(order);
        //向订单明细表插入数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart:
             shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId());

            orderDetailList.add(orderDetail);
        }
        //批量插入
        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //组装返回数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderTime(order.getOrderTime())
                .orderAmount(order.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "商品描述", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
        //将这个 JSON 对象转换为一个 OrderPaymentVO 类型的对象
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        //select * from orders where number = #{outTradeNo}
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        /*
        <update id="update" parameterType="com.sky.entity.Orders">
        update orders
        ......
        where id = #{id}
    </update>
         */
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @Transactional
    public PageResult pageHistory(int page, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(page,pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        //查询到订单记录
        Page<Orders> ordersPage = orderMapper.pageHistory(ordersPageQueryDTO);
        //list存放订单明细，用来展示
        List<OrderVO> list = new ArrayList<>();
        if(ordersPage != null && ordersPage.size() >0){
            for(Orders order : ordersPage){
                Long orderId = order.getId();
                //查询订单明细
                List<OrderDetail> details = orderDetailMapper.getByOrderId(orderId);
                //组装返回数据
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order,orderVO);
                orderVO.setOrderDetailList(details);

                list.add(orderVO);
            }
        }
        //与以往不同的是，显示的内容为 OrderDetail
        return new PageResult(ordersPage.getTotal(),list);
    }

    /**
     * 根据订单id查询订单详情
     * @param id
     * @return
     */
    public OrderVO getByOrderId(Long id) {
        Orders orders = orderMapper.getById(id);

        List<OrderDetail> details = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(details);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id 订单id
     */
    @Transactional
    public void cancelById(Long id) throws Exception{
        //当前订单
        Orders orders = orderMapper.getById(id);
    //无法取消的状态
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(orders.getStatus() > 2 ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //处于待接单（已付款）状态下取消需要退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    orders.getNumber(), //商户订单号
                    orders.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
            //支付状态修改为退款
            orders.setPayStatus(Orders.REFUND);
        }

        Orders newOrders = new Orders();
        newOrders.setId(id);
        newOrders.setStatus(Orders.CANCELLED);  //订单状态
        newOrders.setCancelReason("用户取消");
        newOrders.setCancelTime(LocalDateTime.now());
        //更新数据
        orderMapper.update(newOrders);
    }

    /**
     * 再来一单
     * .map(x -> { ... }) 中的匿名函数/lambda表达式 定义了一个对每个元素进行处理的操作，并且要求返回一个结果对象。
     * 在这个操作中，我们创建了一个 ShoppingCart 对象 shoppingCart，设置了它的属性，并最终返回它
     *
     * @param id 订单id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();
        // 根据订单id查询当前订单详情
        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(id);
        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = detailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //将源订单详情中的菜品放入购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");    //忽略的属性名
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;

            //对象收集
        }).collect(Collectors.toList());
        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理员订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageHistory(ordersPageQueryDTO);
//        部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOlist(page);
        return new PageResult(page.getTotal(),orderVOList);
    }

    /**
     * 各订单状态数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);//待接单
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);    //待派送
        Integer progress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);//派送中

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(progress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders  = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(ordersConfirmDTO.getStatus())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersConfirmDTO
     */
    public void reject(OrdersRejectionDTO ordersConfirmDTO)throws Exception {
        //先查找订单
        Orders orderDB = orderMapper.getById(ordersConfirmDTO.getId());
        //判断订单状态，只有为2 待接单 才能拒绝接单
        if(orderDB == null || !orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //支付状态
        Integer payStatus = orderDB.getPayStatus();
        if(payStatus.equals(Orders.PAID)){
            //退款
            weChatPayUtil.refund(
                    orderDB.getNumber(),
                    orderDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01)
            );
        }
        //更新订单状态
        Orders orders = Orders.builder()
                .id(orderDB.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersConfirmDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now()).build();
        orderMapper.update(orders);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        //支付状态退款
        if(orders.getPayStatus().equals(Orders.PAID)){
            weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01)
            );
        }
        //更新订单状态
        Orders build = Orders.builder()
                .cancelTime(LocalDateTime.now())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .build();
        orderMapper.update(build);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) throws Exception {
        Orders orders = orderMapper.getById(id);
        //检查订单状态
        if(orders == null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders finalOrder = new Orders();
        finalOrder.setId(id);
        finalOrder.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(finalOrder);
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) throws Exception {
        Orders orders = orderMapper.getById(id);
        if(orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders build = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(build);
    }

    /**
     * 根据Page<Orders>组建List<OrderVO>
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVOlist(Page<Orders> page){
        List<OrderVO> list = new ArrayList<>();
        List<Orders> orders = page.getResult();
        //CollectionUtils.isEmpty(orders)判断给定的集合对象是否为空或者为null
        if(!CollectionUtils.isEmpty(orders)){
            for (Orders order: orders) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order,orderVO);
                //给返回对象添加菜品描述
                String detail = getOrderDishesStr(order);
                orderVO.setOrderDishes(detail);
                list.add(orderVO);
            }
        }
        return list;
    }

    /**
     * 添加对菜品的描述
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders){
        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> stringList = detailList.stream().map(x->{
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        //将该订单对应的所有菜品信息拼接在一起
        return String.join("",stringList);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address){
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);
        //获取店铺经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }
        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
