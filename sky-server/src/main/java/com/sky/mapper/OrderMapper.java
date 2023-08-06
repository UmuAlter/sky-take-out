package com.sky.mapper;

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
}
