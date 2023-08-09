package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/*
执行定时任务
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时订单的方法
     * 1待付款
     * 每分钟触发一次
     * 超过15分钟未付款处理
     */
    @Scheduled(cron = "0 * * * * ? *")
    //@Scheduled(cron = "0/5 * * * * ?")
    public void processTimeOutOrder(){
        log.info("处理超时订单:{}", LocalDateTime.now());
        //设置时间
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> timeOutOrders = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT,time);
        if(timeOutOrders != null && timeOutOrders.size() >0){
            for (Orders orders : timeOutOrders) {
                //取消该订单
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直派送中的订单
     * 派送中
     * 每天凌晨一点触发一次
     * 查上一个工作日
     */
    //@Scheduled(cron = "1/5 * * * * ?")
    @Scheduled(cron = "0 0 1 * * ? *")
    public void processDeliveryOrder(){
        log.info("处理一直派送中的订单：{}",LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);
        if(ordersList != null && ordersList.size() >0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
