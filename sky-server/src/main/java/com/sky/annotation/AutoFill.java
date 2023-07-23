package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解
 * 用来对数据库中相同字段进行填充
 */

@Target(ElementType.METHOD) //只在方法上有效
@Retention(RetentionPolicy.RUNTIME) //可以让这个注解在运行时可以被读取和使用
public @interface AutoFill {
    OperationType value();
}
/*
value() 方法名是默认约定的，当只有一个属性时，通常会使用 value() 来命名，以简化使用注解时的代码书写
 */