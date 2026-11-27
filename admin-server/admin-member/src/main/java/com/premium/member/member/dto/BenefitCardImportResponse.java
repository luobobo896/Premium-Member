package com.premium.member.member.dto;

/**
 * 卡密导入结果。
 *
 * @param imported  实际新增数量
 * @param duplicated 已存在或本次重复的数量
 */
public record BenefitCardImportResponse(int imported, int duplicated) {
}
