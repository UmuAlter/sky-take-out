package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    //微信服务接口地址
    public static final String WxLogin =  "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    /**
     * 实现微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //openid为微信中用户的唯一标识
        //获得openid
        String openid = getOpenid(userLoginDTO);

        //判断openid是否为空，空代表失败，抛出业务异常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED );
        }
        //判断是否为新用户 在数据库中查找
        User user = userMapper.getByOpenid(openid);
        if(user == null){
            //为新用户，需要保存到数据表中
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();
            userMapper.insert(user);
        }
        //返回这个用户对象
        return user;
    }

    private String getOpenid(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前用户的OpenId
        Map<String, String> map = new HashMap<>();
        //这四个参数由文档提示
        //https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html

        //小程序 appId
        map.put("appid",weChatProperties.getAppid());
        //小程序 appSecret
        map.put("secret",weChatProperties.getSecret());
        //登录时获取的 code，可通过wx.login获取
        map.put("js_code", userLoginDTO.getCode());
        //授权类型，此处只需填写 authorization_code
        map.put("grant_type","authorization_code");

        //返回的JSON -- fastJson.parseObject -->JSONObject -- .getString() -->字符串
        String json = HttpClientUtil.doGet(WxLogin, map);
        JSONObject jsonObject = JSON.parseObject(json);

        //返回openid
        return jsonObject.getString("openid");
    }
}
