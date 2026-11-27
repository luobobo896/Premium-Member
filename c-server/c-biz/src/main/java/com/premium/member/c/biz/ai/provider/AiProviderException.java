package com.premium.member.c.biz.ai.provider;

import com.premium.member.c.common.exception.ErrorCode;

/**
 * 供应商调用失败。message 会写入任务失败原因，不能包含密钥等敏感信息。
 */
public class AiProviderException extends RuntimeException {

    private final ErrorCode errorCode;
    private final boolean unknown;

    public AiProviderException(ErrorCode errorCode, String message) {
        this(errorCode, message, false);
    }

    public AiProviderException(ErrorCode errorCode, String message, boolean unknown) {
        super(message);
        this.errorCode = errorCode;
        this.unknown = unknown;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public boolean unknown() {
        return unknown;
    }
}
