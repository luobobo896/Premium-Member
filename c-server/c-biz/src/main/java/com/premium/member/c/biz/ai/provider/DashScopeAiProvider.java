package com.premium.member.c.biz.ai.provider;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.premium.member.c.biz.ai.config.DashScopeProperties;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 阿里云百炼 DashScope：文生图、图片编辑、图生视频。
 */
public class DashScopeAiProvider implements AiProvider {

    private static final String IMAGE_PATH = "/api/v1/services/aigc/text2image/image-synthesis";
    private static final String EDIT_PATH = "/api/v1/services/aigc/image2image/image-synthesis";
    private static final String VIDEO_PATH = "/api/v1/services/aigc/video-generation/video-synthesis";
    private static final String TASK_PATH = "/api/v1/tasks/";

    private final DashScopeProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    public DashScopeAiProvider(DashScopeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = RestClient.builder()
                .baseUrl(properties.getBaseUrl().replaceAll("/+$", ""))
                .build();
    }

    @Override
    public String name() {
        return "dashscope";
    }

    @Override
    public AiProviderResult generate(AiProviderRequest request) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY,
                    "DASHSCOPE_API_KEY 未配置");
        }
        Submission submission = submit(request);
        JsonNode completed = poll(submission);
        return download(request.type(), completed, submission);
    }

    private Submission submit(AiProviderRequest request) {
        String path;
        String model;
        Map<String, Object> input = new LinkedHashMap<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        if ("VIDEO".equals(request.type())) {
            path = VIDEO_PATH;
            model = properties.getVideoModel();
            input.put("img_url", requireInputUrl(request));
            input.put("prompt", prompt(request));
            parameters.put("resolution", "480P");
            parameters.put("duration", 5);
        } else if ("DRESSUP".equals(request.type())) {
            path = EDIT_PATH;
            model = properties.getEditModel();
            input.put("function", "description_edit");
            input.put("prompt", prompt(request));
            input.put("base_image_url", requireInputUrl(request));
            parameters.put("n", 1);
        } else {
            path = IMAGE_PATH;
            model = properties.getImageModel();
            input.put("prompt", prompt(request));
            parameters.put("size", "1024*1024");
            parameters.put("n", 1);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("parameters", parameters);

        JsonNode root = post(path, body, model);
        String taskId = root.path("output").path("task_id").asText("");
        if (!StringUtils.hasText(taskId)) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_FAILED,
                    "DashScope 未返回 task_id: " + trace(root, null));
        }
        return new Submission(taskId, root.path("request_id").asText(""), model, request.type());
    }

    private JsonNode poll(Submission submission) {
        Duration timeout = "VIDEO".equals(submission.type())
                ? properties.getVideoTimeout() : properties.getImageTimeout();
        long deadline = System.nanoTime() + timeout.toNanos();
        String requestId = submission.requestId();
        while (System.nanoTime() < deadline) {
            JsonNode root = getTask(submission.taskId());
            requestId = textOr(root, "request_id", requestId);
            JsonNode output = root.path("output");
            String status = output.path("task_status").asText("").toUpperCase(Locale.ROOT);
            if ("SUCCEEDED".equals(status)) {
                return root;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                throw taskFailure(output, submission.model(), submission.taskId(), requestId);
            }
            sleep(properties.getPollInterval());
        }
        throw new AiProviderException(ErrorCode.AI_PROVIDER_TIMEOUT,
                ErrorCode.AI_PROVIDER_TIMEOUT.code() + " " + trace(null, submission.taskId(), requestId),
                true);
    }

    private AiProviderResult download(String type, JsonNode root, Submission submission) {
        JsonNode output = root.path("output");
        String url = "VIDEO".equals(type)
                ? output.path("video_url").asText("")
                : output.path("results").path(0).path("url").asText("");
        if (!StringUtils.hasText(url)) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_FAILED,
                    "DashScope 未返回结果 URL " + trace(root, submission.taskId(),
                            submission.requestId()));
        }
        try {
            ResponseEntity<byte[]> response = client.get().uri(URI.create(url)).retrieve()
                    .toEntity(byte[].class);
            byte[] content = response.getBody();
            if (content == null || content.length == 0) {
                throw new AiProviderException(ErrorCode.AI_PROVIDER_FAILED,
                        "DashScope 结果文件为空 " + trace(root, submission.taskId(),
                                submission.requestId()));
            }
            String mime = mime(response, type);
            String supplierNo = submission.taskId()
                    + (StringUtils.hasText(submission.requestId())
                            ? "|" + submission.requestId() : "");
            return new AiProviderResult(content, mime, extension(mime, type), supplierNo);
        } catch (RestClientResponseException ex) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_FAILED,
                    "DashScope 结果下载失败 HTTP " + ex.getStatusCode().value()
                            + " " + trace(root, submission.taskId(), submission.requestId()));
        } catch (RestClientException ex) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_FAILED,
                    "DashScope 结果下载失败");
        }
    }

    private JsonNode post(String path, Map<String, Object> body, String model) {
        try {
            String json = client.post().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .header("X-DashScope-Async", "enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            return parse(json);
        } catch (RestClientResponseException ex) {
            throw httpFailure(ex, model);
        } catch (RestClientException ex) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY,
                    "DashScope 请求失败: " + sanitize(ex.getMessage()));
        } catch (IOException ex) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY,
                    "DashScope 响应解析失败");
        }
    }

    private JsonNode getTask(String taskId) {
        try {
            String json = client.get().uri(TASK_PATH + taskId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .retrieve()
                    .body(String.class);
            return parse(json);
        } catch (RestClientResponseException ex) {
            throw httpFailure(ex, "");
        } catch (RestClientException ex) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY,
                    "DashScope 任务查询失败: " + sanitize(ex.getMessage()));
        } catch (IOException ex) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY,
                    "DashScope 响应解析失败");
        }
    }

    private AiProviderException httpFailure(RestClientResponseException ex, String model) {
        int status = ex.getStatusCode().value();
        JsonNode root = parseQuietly(ex.getResponseBodyAsString());
        if (status == 401 || status == 403) {
            return new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY,
                    ErrorCode.AI_PROVIDER_NOT_READY.code() + " DashScope HTTP " + status
                            + " " + trace(root, null) + modelSuffix(model));
        }
        return codeFailure(text(root.path("code")), text(root.path("message")), model,
                trace(root, null));
    }

    private AiProviderException taskFailure(JsonNode output, String model, String taskId,
            String requestId) {
        return codeFailure(text(output.path("code")), text(output.path("message")), model,
                trace(null, taskId, requestId));
    }

    private AiProviderException codeFailure(String code, String message, String model) {
        return codeFailure(code, message, model, "");
    }

    private AiProviderException codeFailure(String code, String message, String model,
            String trace) {
        String detail = (StringUtils.hasText(code) ? code + " " : "")
                + (StringUtils.hasText(message) ? message : "")
                + (StringUtils.hasText(trace) ? " " + trace : "");
        detail = sanitize(detail);
        String lower = (code + " " + message).toLowerCase(Locale.ROOT);
        ErrorCode error = ErrorCode.AI_PROVIDER_FAILED;
        if (containsAny(lower, "modelnotopen", "model not open", "not activated", "未开通",
                "accessdenied")) {
            error = ErrorCode.AI_PROVIDER_NOT_ENABLED;
        } else if (containsAny(lower, "datainspection", "data_inspection", "content policy",
                "审核")) {
            error = ErrorCode.AI_CONTENT_REJECTED;
        } else if (containsAny(lower, "throttling", "arrearage", "quota", "rate limit", "限流")) {
            error = ErrorCode.AI_PROVIDER_RATE_LIMITED;
        } else if (containsAny(lower, "failed to download", "download image", "invalidfile",
                "invalid file")) {
            error = ErrorCode.AI_PROVIDER_INPUT_UNREACHABLE;
        } else if (containsAny(lower, "invalidparameter", "invalid parameter", "参数")) {
            error = ErrorCode.AI_PROVIDER_BAD_REQUEST;
        }
        return new AiProviderException(error,
                error.code() + " " + detail + modelSuffix(model));
    }

    private String requireInputUrl(AiProviderRequest request) {
        if (!StringUtils.hasText(request.inputUrl())) {
            throw new AiProviderException(ErrorCode.AI_PROVIDER_INPUT_UNREACHABLE,
                    ErrorCode.AI_PROVIDER_INPUT_UNREACHABLE.code()
                            + " 输入素材缺少公网 URL");
        }
        return request.inputUrl();
    }

    private String prompt(AiProviderRequest request) {
        if (StringUtils.hasText(request.templateName())) {
            return request.templateName();
        }
        if (StringUtils.hasText(request.templateCode())) {
            return request.templateCode();
        }
        return "高质量人像创作";
    }

    private JsonNode parse(String json) throws IOException {
        return objectMapper.readTree(json);
    }

    private JsonNode parseQuietly(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(ErrorCode.AI_PROVIDER_NOT_READY, "AI 任务被中断");
        }
    }

    private String trace(JsonNode root, String taskId) {
        return trace(root, taskId, "");
    }

    private String trace(JsonNode root, String taskId, String requestId) {
        String request = root == null ? requestId : textOr(root, "request_id", requestId);
        return "task_id=" + (taskId == null ? "" : taskId)
                + " request_id=" + (request == null ? "" : request);
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() ? "" : node.asText("");
    }

    private String textOr(JsonNode root, String field, String fallback) {
        String value = root == null ? "" : root.path(field).asText("");
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String modelSuffix(String model) {
        return StringUtils.hasText(model) ? " model=" + model : "";
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String safe = StringUtils.hasText(properties.getApiKey())
                ? value.replace(properties.getApiKey(), "***") : value;
        safe = safe.replaceAll("(?i)(https?://[^\\s?]+)\\?[^\\s]+", "$1?***");
        return safe.replaceAll("(?i)(OSSAccessKeyId|Signature|Expires)=[^\\s&]+", "$1=***");
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String mime(ResponseEntity<byte[]> response, String type) {
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null && StringUtils.hasText(contentType.getType())) {
            return contentType.toString();
        }
        return "VIDEO".equals(type) ? "video/mp4" : "image/png";
    }

    private String extension(String mime, String type) {
        String normalized = mime.toLowerCase(Locale.ROOT);
        if (normalized.contains("jpeg") || normalized.contains("jpg")) {
            return "jpg";
        }
        if (normalized.contains("webp")) {
            return "webp";
        }
        if (normalized.contains("quicktime")) {
            return "mov";
        }
        if (normalized.contains("video")) {
            return "mp4";
        }
        return "VIDEO".equals(type) ? "mp4" : "png";
    }

    private record Submission(String taskId, String requestId, String model, String type) {
    }
}
