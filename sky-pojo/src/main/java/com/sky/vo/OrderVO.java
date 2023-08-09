package com.sky.vo;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends Orders implements Serializable {

    //订单菜品信息
    private String orderDishes;

    private String address;

    private Long addressBookId;

    private BigDecimal amount;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime checkoutTime;

    private String consignee;

    private LocalDateTime deliveryTime;

    private Integer deliveryStatus;

    private LocalDateTime estimatedDeliveryTime;

    private Long id;

    private String number;

    //订单详情
    private List<OrderDetail> orderDetailList;

    private LocalDateTime orderTime;

    private int packAmount;

    private Integer payMethod;

    private Integer payStatus;

    private String phone;

    private String rejectionReason;

    private String remark;

    private Integer status;

    private int tablewareNumber;

    private Integer tablewareStatus;

    private Long userId;

    private String userName;

}
