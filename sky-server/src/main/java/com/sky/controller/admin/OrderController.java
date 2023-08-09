package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Api(tags = "管理端订单相关接口")
@RequestMapping("/admin/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 管理员订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("搜索订单号:{}",ordersPageQueryDTO.getNumber());
        PageResult result = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(result);
    }
    @GetMapping("/statistics")
    @ApiOperation("各订单状态数量统计")
    public Result<OrderStatisticsVO> statistics(){
        log.info("各订单状态数量统计");
        OrderStatisticsVO statisticsVO = orderService.statistics();
        return Result.success(statisticsVO);
    }

    /**
     * 查询订单详情
     * @param id 订单id
     * @return
     */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable("id") Long id){
        log.info("查询订单详情 id:{}",id);
        OrderVO orderVO = orderService.getByOrderId(id);
        return Result.success(orderVO);
    }

    /**
     * 接单
     * @param
     * @return
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单id :{}",ordersConfirmDTO.getId());
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersConfirmDTO) throws Exception{
        log.info("拒单id{}",ordersConfirmDTO.getId());
        orderService.reject(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * - 取消订单 其实就是将订单状态修改为“已取消”
     * - 商家取消订单时需要指定取消原因
     * - 商家取消订单时，如果用户已经完成了支付，需要为用户退款
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception{
        log.info("取消订单 id:{}",ordersCancelDTO.getId());
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable("id") Long id) throws Exception{
        log.info("派送订单id:{}",id);
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * - 完成订单其实就是将订单状态修改为“已完成”
     * - 只有状态为“派送中”的订单可以执行订单完成操作
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable("id") Long id) throws Exception{
        log.info("完成订单id：{}",id);
        orderService.complete(id);
        return Result.success();
    }
}
