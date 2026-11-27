package com.premium.member.framework.audit;

/**
 * 当前请求内补充给审计切面的身份与失败原因。
 */
public final class AuditContext {

    private static final ThreadLocal<Details> CURRENT = new ThreadLocal<>();

    private AuditContext() {
    }

    /**
     * 登录成功后记录操作人。
     *
     * @param userId 用户 ID
     */
    public static void authenticated(long userId) {
        CURRENT.set(new Details(userId, null));
    }

    /**
     * 记录无法从异常码区分的失败原因，如账号不存在与密码错误。
     *
     * @param errorCode 审计原因码
     */
    public static void failure(String errorCode) {
        Details current = CURRENT.get();
        CURRENT.set(new Details(
                current == null ? null : current.userId(),
                errorCode));
    }

    public static Long userId() {
        Details current = CURRENT.get();
        return current == null ? null : current.userId();
    }

    public static String errorCode() {
        Details current = CURRENT.get();
        return current == null ? null : current.errorCode();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private record Details(Long userId, String errorCode) {
    }
}
