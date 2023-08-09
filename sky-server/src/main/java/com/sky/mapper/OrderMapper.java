package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/*
操作orders表
 */
@Mapper
public interface OrderMapper {
    /**
     * 向订单表插入数据
     * @param order
     */
    void insert(Orders order);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询历史订单记录
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单详情
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 根据状态查询订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);
}
