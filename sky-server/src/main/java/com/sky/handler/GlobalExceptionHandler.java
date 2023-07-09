package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 重载
     * 处理SQL中重复用户名的错误
     * 报错信息 Duplicate entry 'zhangsan' for key 'emplyee.idx_username'
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //Duplicate entry 'zhangsan' for key 'emplyee.idx_username'
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] strings = message.split(" ");  //按空格分割
            String username = strings[2];
            String res = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(res);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}
