package com.jin.chat.common.exception;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.service.MonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MonitorService monitorService;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(monitorService);
    }

    @Test
    void handleBusiness_shouldReturnFailResult() {
        BusinessException ex = new BusinessException(ErrorCodeEnum.FORBIDDEN);
        ResultData<Void> result = handler.handleBusiness(ex);
        assertNotEquals(0, result.getCode());
        assertEquals(ErrorCodeEnum.FORBIDDEN.getCode(), result.getCode());
    }

    @Test
    void handleValidation_shouldReturnParamInvalid() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "target");
        binding.addError(new FieldError("target", "username", "用户名不能为空"));
        BindException ex = new BindException(binding);
        ResultData<Void> result = handler.handleValidation(ex);
        assertNotEquals(0, result.getCode());
        assertEquals(ErrorCodeEnum.PARAM_INVALID.getCode(), result.getCode());
        assertTrue(result.getMessage().contains("用户名不能为空"));
    }

    @Test
    void handleUnknown_shouldRecordError() {
        ResultData<Void> result = handler.handleUnknown(new RuntimeException("boom"));
        assertNotEquals(0, result.getCode());
        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), result.getCode());
        verify(monitorService).recordSystemError();
    }

    @Test
    void handleValidation_bindException() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "ao");
        binding.addError(new FieldError("ao", "password", "密码不能为空"));
        BindException ex = new BindException(binding);
        ResultData<Void> result = handler.handleValidation(ex);
        assertTrue(result.getMessage().contains("密码不能为空"));
    }
}
