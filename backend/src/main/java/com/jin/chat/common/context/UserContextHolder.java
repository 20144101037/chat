package com.jin.chat.common.context;

import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;

/**
 * <p>
 * 基于 ThreadLocal 的当前登录用户持有器，由 JWT 拦截器写入、请求结束时清理。
 * </p>
 *
 * @author jinshuai
 */
public final class UserContextHolder {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    public static LoginUser get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前登录用户，未登录抛出业务异常。
     */
    public static LoginUser require() {
        LoginUser user = CONTEXT.get();
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_LOGIN);
        }
        return user;
    }

    public static Long currentUserId() {
        return require().getUserId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
