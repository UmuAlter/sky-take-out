package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/*
操作order_detail表
 */
@Mapper
public interface OrderDetailMapper {
    /**
     * //批量插入
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);
}
