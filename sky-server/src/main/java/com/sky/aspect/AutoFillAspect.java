package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
切面类
实现自动填充逻辑
负责拦截被AutoFill注解的方法
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //切入点
    //拦截范围
    @Pointcut("execution(* com.sky.mapper.*.*(..) ) && @annotation(com.sky.annotation.AutoFill)")
    public void pt(){}

    //在SQL执行前进行填充
    @Before("pt()")
    public void autoFill(JoinPoint joinPoint){
        log.info("自动填充..");
        //获取当前被拦截方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //通过连接点对象获得方法签名
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class); //获得方法签名上的注解
        OperationType value = annotation.value();//获得注解中的属性

        //获得拦截方法的参数，取参数中的实体对象——例如：菜品、员工
        //规定实体对象放在第参数列表的第一位
        Object[] args = joinPoint.getArgs();
        //参数为空的情况
        if(args == null || args.length == 0){
            return;
        }
        Object entity = args[0]; //获得实体

        //准备的赋值数据
        LocalDateTime now = LocalDateTime.now();    //当前时间
        Long currentId = BaseContext.getCurrentId();    //获得当前用户Id
        //向实体对象中填入数据
        if(value == OperationType.INSERT){
            //插入操作
            //getDeclaredMethod获得该类的特定方法 参数为 方法名称 , 方法参数
            try {
                //获得四个方法
                Method setCreateTime = entity.getClass()
                        .getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass()
                        .getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass()
                        .getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass()
                        .getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射对方法对象赋值
                //entity中的setCreateTime方法，参数为now
                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(value == OperationType.UPDATE){
            //更新操作
            try {
                Method setUpdateTime = entity.getClass()
                        .getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass()
                        .getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}