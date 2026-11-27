package com.premium.member.member.provider;

public record WithholdResult(boolean success, String txnNo, String reason) {

    public static WithholdResult ok(String txnNo) {
        return new WithholdResult(true, txnNo, "");
    }

    public static WithholdResult fail(String reason) {
        return new WithholdResult(false, "", reason);
    }
}
