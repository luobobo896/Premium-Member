package com.premium.member.c.biz.ai.storage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.premium.member.c.biz.ai.config.OssProperties;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * 阿里云 OSS 存储，使用 OSS V1 签名，避免额外 SDK 依赖。
 */
public class OssStorageService implements StorageService {

    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    private static final Map<String, String> MIME = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif",
            "mp4", "video/mp4",
            "mov", "video/quicktime",
            "webm", "video/webm");

    private final OssProperties properties;
    private final String baseUrl;
    private final String prefix;
    private final HttpClient httpClient;

    public OssStorageService(OssProperties properties) {
        this.properties = properties;
        this.baseUrl = baseUrl(properties);
        this.prefix = stripSlashes(properties.getPrefix());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public StoredFile store(byte[] content, String extension) {
        return store(content, extension, "misc");
    }

    @Override
    public StoredFile store(byte[] content, String extension, String category) {
        String safeExtension = extension == null ? "bin"
                : extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String key = prefix + "/" + safeCategory(category) + "/"
                + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + safeExtension;
        put(key, content, MIME.getOrDefault(safeExtension, "application/octet-stream"));
        return new StoredFile(key);
    }

    @Override
    public Resource load(String storageKey) {
        HttpRequest request = signedRequest("GET", storageKey, "", null);
        HttpResponse<byte[]> response = send(request);
        return new ByteArrayResource(response.body()) {
            @Override
            public String getFilename() {
                return storageKey;
            }
        };
    }

    @Override
    public void delete(String storageKey) {
        HttpRequest request = signedRequest("DELETE", storageKey, "", null);
        send(request);
    }

    @Override
    public String signedUrl(String storageKey, Duration ttl) {
        long expires = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() + ttl.toSeconds();
        String resource = canonicalResource(storageKey);
        String signature = base64Hmac("GET\n\n\n" + expires + "\n" + resource);
        return objectUrl(storageKey)
                + "?OSSAccessKeyId=" + encode(properties.getAccessKeyId())
                + "&Expires=" + expires
                + "&Signature=" + encode(signature);
    }

    @Override
    public boolean remote() {
        return true;
    }

    private void put(String key, byte[] content, String contentType) {
        HttpRequest request = signedRequest("PUT", key, contentType, content);
        send(request);
    }

    private HttpRequest signedRequest(String method, String storageKey, String contentType,
            byte[] body) {
        String date = HTTP_DATE.format(ZonedDateTime.now(ZoneOffset.UTC));
        String resource = canonicalResource(storageKey);
        String stringToSign = method + "\n\n" + contentType + "\n" + date + "\n" + resource;
        String signature = base64Hmac(stringToSign);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(objectUrl(storageKey)))
                .timeout(Duration.ofSeconds(60))
                .header("Date", date)
                .header("Authorization", "OSS " + properties.getAccessKeyId() + ":" + signature);
        if (StringUtils.hasText(contentType)) {
            builder.header("Content-Type", contentType);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        }
        return builder.build();
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "OSS 请求失败 HTTP " + response.statusCode());
            }
            return response;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS 请求失败");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS 请求被中断");
        }
    }

    private String base64Hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(properties.getAccessKeySecret()
                    .getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OSS 签名失败");
        }
    }

    private String canonicalResource(String storageKey) {
        return "/" + properties.getBucket() + "/" + storageKey;
    }

    private String objectUrl(String storageKey) {
        return baseUrl + "/" + encodeKey(storageKey);
    }

    private static String baseUrl(OssProperties properties) {
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        if (properties.isPathStyle()) {
            return endpoint + "/" + properties.getBucket();
        }
        URI uri = URI.create(endpoint);
        return uri.getScheme() + "://" + properties.getBucket() + "." + uri.getAuthority();
    }

    private static String stripSlashes(String value) {
        return value == null ? "" : value.replaceAll("^/+|/+$", "");
    }

    private static String safeCategory(String category) {
        return StringUtils.hasText(category)
                ? category.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "")
                : "misc";
    }

    private static String encodeKey(String key) {
        String[] parts = key.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (String part : parts) {
            if (!encoded.isEmpty()) {
                encoded.append('/');
            }
            encoded.append(encode(part));
        }
        return encoded.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
