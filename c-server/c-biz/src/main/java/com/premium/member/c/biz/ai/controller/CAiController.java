package com.premium.member.c.biz.ai.controller;

import java.util.List;

import com.premium.member.c.biz.ai.dto.AiAssetResponse;
import com.premium.member.c.biz.ai.dto.AiTaskCreateRequest;
import com.premium.member.c.biz.ai.dto.AiTaskResponse;
import com.premium.member.c.biz.ai.dto.AiTemplateResponse;
import com.premium.member.c.biz.ai.service.AiAssetService;
import com.premium.member.c.biz.ai.service.AiTaskService;
import com.premium.member.c.biz.ai.service.AiTaskWorker;
import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * C 端 AI 创作：模板、上传、任务与作品。
 */
@RestController
@RequestMapping("/c/ai")
public class CAiController {

    private static final String FILE_PREFIX = "/c/ai/files/";

    private final AiTaskService taskService;
    private final AiAssetService assetService;
    private final AiTaskWorker taskWorker;

    public CAiController(AiTaskService taskService, AiAssetService assetService,
            AiTaskWorker taskWorker) {
        this.taskService = taskService;
        this.assetService = assetService;
        this.taskWorker = taskWorker;
    }

    @GetMapping("/templates")
    public R<List<AiTemplateResponse>> templates(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        return R.ok(taskService.templates(type, category));
    }

    @PostMapping("/assets")
    public R<AiAssetResponse> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(assetService.upload(SecurityUtils.currentUser().phone(), file));
    }

    @PostMapping("/tasks")
    public R<AiTaskResponse> create(@Valid @RequestBody AiTaskCreateRequest request) {
        AiTaskResponse task = taskService.create(SecurityUtils.currentUser().phone(), request);
        taskWorker.execute(task.taskNo());
        return R.ok(task);
    }

    @GetMapping("/tasks/{taskNo}")
    public R<AiTaskResponse> detail(@PathVariable String taskNo) {
        return R.ok(taskService.detail(SecurityUtils.currentUser().phone(), taskNo));
    }

    @GetMapping("/tasks")
    public R<List<AiTaskResponse>> tasks(@RequestParam(required = false) String status) {
        return R.ok(taskService.list(SecurityUtils.currentUser().phone(), status));
    }

    @DeleteMapping("/works/{id}")
    public R<Void> deleteWork(@PathVariable long id) {
        taskService.deleteWork(SecurityUtils.currentUser().phone(), id);
        return R.ok();
    }

    @GetMapping("/files/**")
    public ResponseEntity<Resource> file(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(FILE_PREFIX);
        String key = index < 0 ? "" : uri.substring(index + FILE_PREFIX.length());
        Resource resource = assetService.load(key);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                .body(resource);
    }
}
