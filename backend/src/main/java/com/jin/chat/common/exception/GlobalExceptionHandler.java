package com.jin.chat.common.exception;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * <p>
 * 全局异常处理器。系统级异常同时计入监控错误率指标。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MonitorService monitorService;

    public GlobalExceptionHandler(@Lazy MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @ExceptionHandler(BusinessException.class)
    public ResultData<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ResultData.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResultData<Void> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return ResultData.fail(ErrorCodeEnum.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public ResultData<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        monitorService.recordSystemError();
        return ResultData.fail(ErrorCodeEnum.SYSTEM_ERROR);
    }
}
