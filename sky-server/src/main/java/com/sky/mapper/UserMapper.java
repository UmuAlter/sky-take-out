package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查找用户
     * 数据库中openid字段为String
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 注册一个新用户（插入数据）
     * @param user
     */
    void insert(User user);

    /**
     * 根据用户id查找用户
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);
}
