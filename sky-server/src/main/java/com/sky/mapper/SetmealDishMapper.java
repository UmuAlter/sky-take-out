package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/*
套餐
 */
@Mapper
public interface SetmealDishMapper {
    //多个或集合形式参数的在SQL中要用foreach
    List<Long> getSetmealIdsByDishIds(List<Long> DishIds);
}
