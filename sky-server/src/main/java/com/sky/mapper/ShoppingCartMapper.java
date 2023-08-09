package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 根据用户id 菜品id 套餐id 口味
     * 查找菜品
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据id修改商品数量
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart cart);

    /**
     * 向购物车插入数据
     * @param shoppingCart
     */
    @Insert("insert into " +
    "shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time)" +
    "values (#{name},#{image},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户ID清空购物车
     * @param id
     */
    @Delete("delete from shopping_cart where user_id = #{id}")
    void deleteByUserId(Long id);

    /**
     * 根据Id删除某商品
     * @param id
     */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    /**
     * 将购物车对象批量添加到数据库
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
