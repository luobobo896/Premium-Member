package com.premium.member.c.biz.ai.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import com.premium.member.c.biz.ai.entity.AiTask;
import com.premium.member.c.biz.ai.entity.AiTemplate;
import com.premium.member.c.biz.ai.entity.Asset;
import com.premium.member.c.biz.ai.mapper.AiMapper;
import com.premium.member.c.biz.ai.provider.AiProvider;
import com.premium.member.c.biz.ai.provider.AiProviderRequest;
import com.premium.member.c.biz.ai.provider.AiProviderResult;
import com.premium.member.c.biz.ai.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 异步执行 AI 任务。终态提交委托给事务服务，避免在供应商等待期间占用数据库事务。
 */
@Service
public class AiTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(AiTaskWorker.class);

    private final AiMapper aiMapper;
    private final AiProvider aiProvider;
    private final AiTaskSettlementService settlementService;
    private final StorageService storageService;
    private final ThreadPoolTaskExecutor executor;
    private final boolean mockControlEnabled;

    public AiTaskWorker(AiMapper aiMapper, AiProvider aiProvider,
            AiTaskSettlementService settlementService, StorageService storageService,
            @Qualifier("aiTaskExecutor") ThreadPoolTaskExecutor executor, Environment environment) {
        this.aiMapper = aiMapper;
        this.aiProvider = aiProvider;
        this.settlementService = settlementService;
        this.storageService = storageService;
        this.executor = executor;
        this.mockControlEnabled = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
    }

    public void execute(String taskNo) {
        executor.execute(() -> run(taskNo));
    }

    private void run(String taskNo) {
        if (aiMapper.markRunning(taskNo) != 1) {
            return;
        }
        AiTask task = aiMapper.selectAnyTaskByNo(taskNo);
        if (task == null) {
            return;
        }
        try {
            String mockResult = mockControlEnabled ? task.getMockResult() : "";
            if ("UNKNOWN".equals(mockResult)) {
                settlementService.unknown(task, "Mock 模拟供应商结果未知");
                return;
            }
            if ("FAIL".equals(mockResult) || "REJECT".equals(mockResult)) {
                settlementService.failure(task, "FAIL".equals(mockResult) ? "FAILED" : "REJECTED",
                        "Mock 模拟" + ("FAIL".equals(mockResult) ? "失败" : "拒绝"));
                return;
            }
            AiTemplate template = aiMapper.selectActiveTemplate(task.getTemplateId());
            Asset input = task.getInputAssetId() == null ? null
                    : aiMapper.selectAsset(task.getInputAssetId());
            byte[] inputBytes = input == null ? new byte[0] : read(input.getStorageKey());
            AiProviderResult result = aiProvider.generate(new AiProviderRequest(
                    task.getTaskNo(), task.getType(),
                    template == null ? "" : template.getCode(),
                    template == null ? "" : template.getName(),
                    input == null ? "" : input.getMime(), inputBytes));
            StorageService.StoredFile stored = storageService.store(result.content(), result.extension());
            settlementService.success(task, stored, result);
        } catch (RuntimeException ex) {
            log.warn("AI task failed: taskNo={}, reason={}", taskNo, ex.getMessage());
            settlementService.failure(task, "FAILED", ex.getMessage() == null
                    ? "供应商调用失败" : ex.getMessage());
        }
    }

    private byte[] read(String storageKey) {
        try {
            Resource resource = storageService.load(storageKey);
            return resource.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("输入素材读取失败", ex);
        }
    }
}
