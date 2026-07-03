package com.jin.chat.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void shouldUseErrorCodeMessage() {
        BusinessException ex = new BusinessException(ErrorCodeEnum.USERNAME_EXIST);
        assertEquals(ErrorCodeEnum.USERNAME_EXIST.getCode(), ex.getCode());
        assertEquals(ErrorCodeEnum.USERNAME_EXIST.getMessage(), ex.getMessage());
    }

    @Test
    void shouldUseCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCodeEnum.PARAM_INVALID, "字段错误");
        assertEquals(ErrorCodeEnum.PARAM_INVALID.getCode(), ex.getCode());
        assertEquals("字段错误", ex.getMessage());
    }

    @Test
    void shouldUseRawCode() {
        BusinessException ex = new BusinessException(9999, "自定义");
        assertEquals(9999, ex.getCode());
        assertEquals("自定义", ex.getMessage());
    }
}
