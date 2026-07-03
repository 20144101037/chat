package com.jin.chat.common.api;

import com.jin.chat.common.exception.ErrorCodeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 统一响应结构体。
 * </p>
 *
 * @author jinshuai
 */
@Data
public class ResultData<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 链路追踪 ID */
    private String traceId;

    private long timestamp = System.currentTimeMillis();

    public static <T> ResultData<T> success() {
        return success(null);
    }

    public static <T> ResultData<T> success(T data) {
        ResultData<T> result = new ResultData<>();
        result.setCode(ErrorCodeEnum.SUCCESS.getCode());
        result.setMessage(ErrorCodeEnum.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> ResultData<T> fail(int code, String message) {
        ResultData<T> result = new ResultData<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> ResultData<T> fail(ErrorCodeEnum errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }
}
