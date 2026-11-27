package com.premium.member.c.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * C 端统一响应体。
 *
 * @param code    0 表示成功，-1 表示失败
 * @param data    业务数据
 * @param error   错误信息，成功时为 null
 * @param message 提示信息
 * @param bizCode 机器可读业务错误码
 * @param <T>     数据类型
 */
public record R<T>(
        int code,
        T data,
        String error,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String bizCode) {

    public static <T> R<T> ok(T data) {
        return new R<>(0, data, null, "ok", null);
    }

    public static R<Void> ok() {
        return new R<>(0, null, null, "ok", null);
    }

    public static <T> R<T> fail(String bizCode, String message) {
        return new R<>(-1, null, message, message, bizCode);
    }
}
