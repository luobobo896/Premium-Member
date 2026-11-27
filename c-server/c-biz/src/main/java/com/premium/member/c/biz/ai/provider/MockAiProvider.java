package com.premium.member.c.biz.ai.provider;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地占位供应商：异步延时后生成图片或 1 秒视频占位文件。
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "mock",
        matchIfMissing = true)
public class MockAiProvider implements AiProvider {

    private static final byte[] PLACEHOLDER_VIDEO = Base64.getDecoder().decode(
            "AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAMXbW9vdgAAAGxtdmhkAAAAAAAAAAAAAAAA"
            + "AAAD6AAAA+gAAQAAAQAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAA"
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAkF0cmFrAAAAXHRraGQAAAADAAAAAAAAAAAAAAAB"
            + "AAAAAAAAA+gAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAA"
            + "AUAAAAHgAAAAAAAkZWR0cwAAABxlbHN0AAAAAAAAAAEAAAPoAAAAAAABAAAAAAG5bWRpYQAAACBt"
            + "ZGhkAAAAAAAAAAAAAAAAAABAAAAAQABVxAAAAAAALWhkbHIAAAAAAAAAAHZpZGUAAAAAAAAAAAAAAABW"
            + "aWRlb0hhbmRsZXIAAAABZG1pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAA"
            + "AAAAAQAAAAx1cmwgAAAAAQAAASRzdGJsAAAAwHN0c2QAAAAAAAAAAQAAALBhdmMxAAAAAAAAAAEAAAAA"
            + "AAAAAAAAAAAAAAAAAUAB4ABIAAAASAAAAAAAAAABFUxhdmM2My4yNC4xMDAgbGlieDI2NAAAAAAAAAAA"
            + "AAAAGP//AAAANmF2Y0MBZAAV/+EAGWdkABWs2UFA9sBEAAADAAQAAAMACDxYtlgBAAZo6+PLIsD9+PgA"
            + "AAAAEHBhc3AAAAABAAAAAQAAABRidHJ0AAAAAAAAGDgAAAAAAAAAGHN0dHMAAAAAAAAAAQAAAAEAAEAA"
            + "AAAAHHN0c2MAAAAAAAAAAQAAAAEAAAABAAAAAQAAABRzdHN6AAAAAAAAAwcAAAABAAAAFHN0Y28AAAAA"
            + "AAAAAQAAA0cAAABidWR0YQAAAFptZXRhAAAAAAAAACFoZGxyAAAAAAAAAABtZGlyYXBwbAAAAAAAAAAA"
            + "AAAAAC1pbHN0AAAAJal0b28AAAAdZGF0YQAAAAEAAAAATGF2ZjYzLjE2LjEwMAAAAAhmcmVlAAADD21k"
            + "YXQAAAKuBgX//6rcRem95tlIt5Ys2CDZI+7veDI2NCAtIGNvcmUgMTY1IHIzMjIyIGIzNTYwNWEgLSBI"
            + "LjI2NC9NUEVHLTQgQVZDIGNvZGVjIC0gQ29weWxlZnQgMjAwMy0yMDI1IC0gaHR0cDovL3d3dy52aWRl"
            + "b2xhbi5vcmcveDI2NC5odG1sIC0gb3B0aW9uczogY2FiYWM9MSByZWY9MyBkZWJsb2NrPTE6MDowIGFu"
            + "YWx5c2U9MHgzOjB4MTEzIG1lPWhleCBzdWJtZT03IHBzeT0xIHBzeV9yZD0xLjAwOjAuMDAgbWl4ZWRf"
            + "cmVmPTEgbWVfcmFuZ2U9MTYgY2hyb21hX21lPTEgdHJlbGxpcz0xIDh4OGRjdD0xIGNxbT0wIGRlYWR6"
            + "b25lPTIxLDExIGZhc3RfcHNraXA9MSBjaHJvbWFfcXBfb2Zmc2V0PS0yIHRocmVhZHM9MTUgbG9va2Fo"
            + "ZWFkX3RocmVhZHM9MiBzbGljZWRfdGhyZWFkcz0wIG5yPTAgZGVjaW1hdGU9MSBpbnRlcmxhY2VkPTAg"
            + "Ymx1cmF5X2NvbXBhdD0wIGNvbnN0cmFpbmVkX2ludHJhPTAgYmZyYW1lcz0zIGJfcHlyYW1pZD0yIGJf"
            + "YWRhcHQ9MSBiX2JpYXM9MCBkaXJlY3Q9MSB3ZWlnaHRiPTEgb3Blbl9nb3A9MCB3ZWlnaHRwPTIga2V5"
            + "aW50PTI1MCBrZXlpbnRfbWluPTEgc2NlbmVjdXQ9NDAgaW50cmFfcmVmcmVzaD0wIHJjX2xvb2thaGVh"
            + "ZD00MCByYz1jcmYgbWJ0cmVlPTEgY3JmPTIzLjAgcWNvbXA9MC42MCBxcG1pbj0wIHFwbWF4PTY5IHFw"
            + "c3RlcD00IGlwX3JhdGlvPTEuNDAgYXE9MToxLjAwAIAAAABRZYiEABX//uzPfgU16GMuU41H56deOY06"
            + "aGeK6v9N6hcRjDjyW4ADcS20LxOJaF3F/wElAAAHgEJji2UyWbxYSTUAAAMAAAMAAAMAAAMAABWR");

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public AiProviderResult generate(AiProviderRequest request) {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 任务被中断", ex);
        }
        if ("VIDEO".equals(request.type())) {
            return new AiProviderResult(PLACEHOLDER_VIDEO, "video/mp4", "mp4",
                    "MOCK-" + request.taskNo());
        }
        return new AiProviderResult(renderImage(request), "image/png", "png",
                "MOCK-" + request.taskNo());
    }

    private byte[] renderImage(AiProviderRequest request) {
        int width = 900;
        int height = 1200;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int hue = Math.abs(request.templateCode().hashCode()) % 360;
            Color start = Color.getHSBColor(hue / 360f, 0.42f, 0.78f);
            Color end = Color.getHSBColor(((hue + 48) % 360) / 360f, 0.55f, 0.45f);
            graphics.setPaint(new GradientPaint(0, 0, start, width, height, end));
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(new Color(255, 255, 255, 72));
            graphics.fillRoundRect(70, 70, width - 140, height - 140, 36, 36);
            graphics.setColor(new Color(255, 255, 255, 190));
            graphics.setStroke(new java.awt.BasicStroke(3f));
            graphics.drawRoundRect(70, 70, width - 140, height - 140, 36, 36);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("PingFang SC", Font.BOLD, 54));
            drawCentered(graphics, request.templateName(), width, 560);
            graphics.setFont(new Font("PingFang SC", Font.PLAIN, 28));
            drawCentered(graphics, "AI 创作占位图 · " + request.taskNo(), width, 630);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 22));
            drawCentered(graphics, request.templateCode(), width, 1080);
        } finally {
            graphics.dispose();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("占位图生成失败", ex);
        }
    }

    private void drawCentered(Graphics2D graphics, String text, int width, int y) {
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, (width - textWidth) / 2, y);
    }
}
