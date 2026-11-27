package com.premium.member.system.dto;

/**
 * 服务端验证码响应。
 *
 * @param captchaId   验证码 ID
 * @param imageBase64 PNG 图片的 data URL
 */
public record CaptchaResponse(String captchaId, String imageBase64) {
}
