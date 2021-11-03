package com.sd.task.controller.adivce;

import com.sd.task.pojo.dto.JSONResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class ControllerAdvice {
    /**
     * 暂时没有弄一个通用处理类,先放着
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public String exceptionHandler(Exception e) {
        return JSONResult.fillResultString(5000, e.getMessage(), null);
    }

    /**
     * 拦截表单参数校验
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({BindException.class})
    public String bindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String msg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        log.error("参数校验异常拦截：{}", msg);
        return JSONResult.fillResultString(0, msg, null);
    }

    /**
     * 拦截JSON参数校验
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String bindException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String msg = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        log.error("参数校验异常拦截：{}", msg);
        return JSONResult.fillResultString(0, msg, null);
    }

}
