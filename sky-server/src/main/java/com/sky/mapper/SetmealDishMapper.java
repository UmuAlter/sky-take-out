package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/*
套餐
 */
@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品Id获得套餐Id
     * @param DishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> DishIds);

    /**
     * 保存套餐和菜品的联系
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐Id删除 套餐——菜品 关系
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据根据套餐Id查询 套餐——菜品 关系
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);
}
