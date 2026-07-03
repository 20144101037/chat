package com.jin.chat.common.exception;

import lombok.Getter;

/**
 * <p>
 * 业务异常，由全局异常处理器统一转换为 ResultData。
 * </p>
 *
 * @author jinshuai
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
