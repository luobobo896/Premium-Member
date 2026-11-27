package com.premium.member.c.biz.ai.storage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.premium.member.c.biz.ai.config.OssProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OssStorageServiceTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void storesLoadsSignsAndDeletesWithStubOss() throws Exception {
        Map<String, byte[]> objects = new ConcurrentHashMap<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> handle(exchange, objects, authorization));
        server.start();

        OssProperties properties = new OssProperties();
        properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setBucket("demopt");
        properties.setAccessKeyId("test-ak");
        properties.setAccessKeySecret("test-secret");
        properties.setPrefix("premium-member/ai/test");
        properties.setPathStyle(true);
        OssStorageService storage = new OssStorageService(properties);

        byte[] content = "oss-content".getBytes(StandardCharsets.UTF_8);
        StorageService.StoredFile stored = storage.store(content, "png", "input");

        assertTrue(stored.storageKey().startsWith("premium-member/ai/test/input/"));
        assertTrue(stored.storageKey().endsWith(".png"));
        assertTrue(authorization.get().startsWith("OSS test-ak:"));
        assertFalse(authorization.get().contains("test-secret"));
        assertArrayEquals(content, storage.load(stored.storageKey()).getInputStream().readAllBytes());

        String signed = storage.signedUrl(stored.storageKey(), Duration.ofMinutes(10));
        assertTrue(signed.contains("OSSAccessKeyId=test-ak"));
        assertTrue(signed.contains("Expires="));
        assertTrue(signed.contains("Signature="));
        assertFalse(signed.contains("test-secret"));

        storage.delete(stored.storageKey());
        assertNull(objects.get("/demopt/" + stored.storageKey()));
    }

    private static void handle(HttpExchange exchange, Map<String, byte[]> objects,
            AtomicReference<String> authorization) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        if ("PUT".equals(method)) {
            objects.put(path, exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(200, -1);
        } else if ("GET".equals(method)) {
            byte[] body = objects.get(path);
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        } else if ("DELETE".equals(method)) {
            objects.remove(path);
            exchange.sendResponseHeaders(204, -1);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
        exchange.close();
    }
}
