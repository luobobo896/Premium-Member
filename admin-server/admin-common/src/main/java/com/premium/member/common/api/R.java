package com.premium.member.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 与 vben 前端一致的基础响应体。
 *
 * @param code    0 表示成功，-1 表示失败
 * @param data    业务数据
 * @param error   错误信息，成功时为 null
 * @param message 提示信息
 * @param bizCode 机器可读业务错误码，成功时不输出
 * @param <T>     数据类型
 */
public record R<T>(
        int code,
        T data,
        String error,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String bizCode) {

    /**
     * 成功响应。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> R<T> ok(T data) {
        return new R<>(0, data, null, "ok", null);
    }

    public static R<Void> ok() {
        return new R<>(0, null, null, "ok", null);
    }

    /**
     * 失败响应。
     *
     * @param bizCode 业务错误码
     * @param message 展示给用户的信息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> R<T> fail(String bizCode, String message) {
        return new R<>(-1, null, message, message, bizCode);
    }
}
