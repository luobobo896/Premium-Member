package com.premium.member.c.biz.ai.provider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premium.member.c.biz.ai.config.DashScopeProperties;
import com.premium.member.c.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashScopeAiProviderTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generatesImageAndKeepsSupplierIds() throws Exception {
        byte[] image = "png-bytes".getBytes(StandardCharsets.UTF_8);
        String base = start(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/text2image/image-synthesis")) {
                respond(exchange, 200, """
                        {"request_id":"req-image","output":{"task_id":"task-image"}}
                        """);
            } else if (path.endsWith("/api/v1/tasks/task-image")) {
                respond(exchange, 200, """
                        {"request_id":"req-image","output":{"task_status":"SUCCEEDED",
                        "results":[{"url":"%s/result.png"}]}}
                        """.formatted(baseUrl()));
            } else {
                respondBytes(exchange, 200, "image/png", image);
            }
        });

        AiProviderResult result = provider(Duration.ofSeconds(1)).generate(
                request("PHOTO", null));

        assertArrayEquals(image, result.content());
        assertEquals("image/png", result.mime());
        assertEquals("png", result.extension());
        assertEquals("task-image|req-image", result.supplierTaskNo());
    }

    @Test
    void sendsVideoParametersAndDownloadsVideo() throws Exception {
        byte[] video = "mp4-bytes".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> submitBody = new AtomicReference<>();
        String base = start(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/video-generation/video-synthesis")) {
                submitBody.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(exchange, 200, """
                        {"request_id":"req-video","output":{"task_id":"task-video"}}
                        """);
            } else if (path.endsWith("/api/v1/tasks/task-video")) {
                respond(exchange, 200, """
                        {"request_id":"req-video","output":{"task_status":"SUCCEEDED",
                        "video_url":"%s/result.mp4"}}
                        """.formatted(baseUrl()));
            } else {
                respondBytes(exchange, 200, "video/mp4", video);
            }
        });

        AiProviderResult result = provider(Duration.ofSeconds(1)).generate(
                request("VIDEO", base + "/input.png"));

        assertArrayEquals(video, result.content());
        assertEquals("mp4", result.extension());
        assertTrue(submitBody.get().contains("\"img_url\":\"" + base + "/input.png\""));
        assertTrue(submitBody.get().contains("\"resolution\":\"480P\""));
        assertTrue(submitBody.get().contains("\"duration\":5"));
    }

    @Test
    void sendsEditFunction() throws Exception {
        AtomicReference<String> submitBody = new AtomicReference<>();
        String base = start(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/image2image/image-synthesis")) {
                submitBody.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(exchange, 200, """
                        {"request_id":"req-edit","output":{"task_id":"task-edit"}}
                        """);
            } else if (path.endsWith("/api/v1/tasks/task-edit")) {
                respond(exchange, 200, """
                        {"request_id":"req-edit","output":{"task_status":"SUCCEEDED",
                        "results":[{"url":"%s/result.png"}]}}
                        """.formatted(baseUrl()));
            } else {
                respondBytes(exchange, 200, "image/png", new byte[] {1});
            }
        });

        provider(Duration.ofSeconds(1)).generate(request("DRESSUP", base + "/input.png"));

        assertTrue(submitBody.get().contains("\"function\":\"description_edit\""));
        assertTrue(submitBody.get().contains("\"base_image_url\":\"" + base + "/input.png\""));
    }

    @Test
    void mapsUnauthorizedToNotReady() throws Exception {
        start(exchange -> respond(exchange, 401, "{\"message\":\"bad key\"}"));

        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider(Duration.ofSeconds(1)).generate(request("PHOTO", null)));

        assertEquals(ErrorCode.AI_PROVIDER_NOT_READY, error.errorCode());
        assertFalse(error.getMessage().contains("test-key"));
    }

    @Test
    void mapsModelNotOpenToNotEnabledWithModelName() throws Exception {
        start(exchange -> respond(exchange, 400, """
                {"request_id":"req-model","code":"ModelNotOpen","message":"model is not open"}
                """));

        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider(Duration.ofSeconds(1)).generate(request("PHOTO", null)));

        assertEquals(ErrorCode.AI_PROVIDER_NOT_ENABLED, error.errorCode());
        assertTrue(error.getMessage().contains("wanx2.1-t2i-turbo"));
        assertTrue(error.getMessage().contains("req-model"));
    }

    @Test
    void mapsThrottlingToRateLimited() throws Exception {
        start(exchange -> respond(exchange, 429, """
                {"request_id":"req-rate","code":"Throttling","message":"rate quota"}
                """));

        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider(Duration.ofSeconds(1)).generate(request("PHOTO", null)));

        assertEquals(ErrorCode.AI_PROVIDER_RATE_LIMITED, error.errorCode());
    }

    @Test
    void mapsInputDownload403ToUnreachable() throws Exception {
        start(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/video-generation/video-synthesis")) {
                respond(exchange, 200, """
                        {"request_id":"req-dl","output":{"task_id":"task-dl"}}
                        """);
            } else {
                respond(exchange, 200, """
                        {"request_id":"req-dl","output":{"task_status":"FAILED",
                        "code":"InvalidParameter","message":"Failed to download image: 403 http://example.test/input.png?OSSAccessKeyId=ak&Signature=secret"}}
                        """);
            }
        });

        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider(Duration.ofSeconds(1)).generate(
                        request("VIDEO", "http://example.test/input.png")));

        assertEquals(ErrorCode.AI_PROVIDER_INPUT_UNREACHABLE, error.errorCode());
        assertFalse(error.getMessage().contains("Signature=secret"));
        assertFalse(error.getMessage().contains("OSSAccessKeyId=ak"));
    }

    @Test
    void mapsInvalidParameterToBadRequest() throws Exception {
        start(exchange -> respond(exchange, 400, """
                {"request_id":"req-bad","code":"InvalidParameter","message":"bad size"}
                """));

        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider(Duration.ofSeconds(1)).generate(request("PHOTO", null)));

        assertEquals(ErrorCode.AI_PROVIDER_BAD_REQUEST, error.errorCode());
    }

    @Test
    void timeoutIsUnknownAndKeepsTaskAndRequestIds() throws Exception {
        start(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/text2image/image-synthesis")) {
                respond(exchange, 200, """
                        {"request_id":"req-slow","output":{"task_id":"task-slow"}}
                        """);
            } else {
                respond(exchange, 200, """
                        {"request_id":"req-slow","output":{"task_status":"RUNNING"}}
                        """);
            }
        });

        AiProviderException error = assertThrows(AiProviderException.class,
                () -> provider(Duration.ofMillis(50)).generate(request("PHOTO", null)));

        assertEquals(ErrorCode.AI_PROVIDER_TIMEOUT, error.errorCode());
        assertTrue(error.unknown());
        assertTrue(error.getMessage().contains("task_id=task-slow"));
        assertTrue(error.getMessage().contains("request_id=req-slow"));
    }

    private DashScopeAiProvider provider(Duration imageTimeout) {
        DashScopeProperties properties = new DashScopeProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl(baseUrl());
        properties.setPollInterval(Duration.ofMillis(1));
        properties.setImageTimeout(imageTimeout);
        properties.setVideoTimeout(imageTimeout);
        return new DashScopeAiProvider(properties, new ObjectMapper());
    }

    private AiProviderRequest request(String type, String inputUrl) {
        return new AiProviderRequest("AI-TEST", type, "PHOTO_REAL", "真实照片",
                "image/png", new byte[0], inputUrl);
    }

    private String start(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
        return baseUrl();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        respondBytes(exchange, status, "application/json",
                body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respondBytes(HttpExchange exchange, int status, String contentType,
            byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
