package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/*
通用接口
 */
@RestController
@Slf4j
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {
    @Autowired
    private AliOssUtil util;

    //文件上传
    //前端传来的是名字为file的文件，所以用MultipartFile file接受
    //参数名要一致，不一致用@RequestParam
    @PostMapping("/upload")
    @ApiOperation(value = "文件上传")
    public Result<String> upload( MultipartFile file){
        log.info("文件上传");
        try {
            //计算文件的UUID，防止文件重名导致的文件覆盖
            //原始名
            String originalFilename = file.getOriginalFilename();
            //从后缀开始截取
            String substring = originalFilename.substring(originalFilename.lastIndexOf("."));
            //生成UUID
            String newName = UUID.randomUUID().toString() + substring;

            //调用阿里云工具类实现文件上传
            //util.upload(file.getBytes(), newName);

            //本地上传 存在D:\JavaPractice\Java_Web\ImageOrder
            file.transferTo(new File("D:\\JavaPractice\\Java_Web\\ImageOrder\\"+newName));

        } catch (Exception e) {
            log.error("文件上传失败");
        }
        //文件上传成功
        return Result.success();
    }
}
