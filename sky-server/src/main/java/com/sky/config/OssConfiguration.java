package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
阿里云配置类
创建一个文件上传工具类对象
 */
@Configuration  //项目启动时就会调用该方法
@Slf4j
public class OssConfiguration {
    //AliOssProperties交给了spring boot管理

    //@ConditionalOnMissingBean当容器中不存在某个 Bean 的情况下，才会执行被注解的方法或加载被注解的配置类
    //确保了AliOssUtil在容器中只存在一个，一个就够了
    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties properties){
        log.info("创建一个文件上传工具类对象");
        return new AliOssUtil(properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret(),
                properties.getBucketName()
        );
    }
}
