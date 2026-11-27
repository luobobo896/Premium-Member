package com.premium.member.c.biz.ai.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.premium.member.c.biz.ai.entity.AiTask;
import com.premium.member.c.biz.ai.entity.Asset;
import com.premium.member.c.biz.ai.mapper.AiMapper;
import com.premium.member.c.biz.ai.provider.AiProviderResult;
import com.premium.member.c.biz.ai.storage.StorageService;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务终态、产出资产与额度账本在同一事务提交。
 */
@Service
public class AiTaskSettlementService {

    private final AiMapper aiMapper;
    private final AiQuotaService quotaService;
    private final StorageService storageService;

    public AiTaskSettlementService(AiMapper aiMapper, AiQuotaService quotaService,
            StorageService storageService) {
        this.aiMapper = aiMapper;
        this.quotaService = quotaService;
        this.storageService = storageService;
    }

    @Transactional
    public void success(AiTask task, StorageService.StoredFile stored, AiProviderResult result) {
        Asset asset = new Asset();
        asset.setOwnerMemberId(task.getMemberId());
        asset.setPurpose("OUTPUT");
        asset.setMime(result.mime());
        asset.setSize((long) result.content().length);
        asset.setStorageKey(stored.storageKey());
        asset.setStatus("ACTIVE");
        asset.setExpireAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
        aiMapper.insertAsset(asset);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (aiMapper.markSuccess(task.getTaskNo(), asset.getId(), result.supplierTaskNo(), now) != 1) {
            storageService.delete(stored.storageKey());
            throw new BusinessException(ErrorCode.AI_TASK_STATE_INVALID);
        }
        // 积分在提交时已扣除，成功结算不再产生新的积分流水。
    }

    @Transactional
    public void failure(AiTask task, String status, String reason) {
        String safeReason = reason == null ? "" : reason.substring(0, Math.min(reason.length(), 512));
        if (aiMapper.markTerminal(task.getTaskNo(), status, safeReason,
                OffsetDateTime.now(ZoneOffset.UTC)) == 1) {
            quotaService.refund(task.getMemberId(), task.getType(), task.getTaskNo());
        }
    }

    @Transactional
    public void unknown(AiTask task, String reason) {
        String safeReason = reason == null ? "" : reason.substring(0, Math.min(reason.length(), 512));
        aiMapper.markTerminal(task.getTaskNo(), "UNKNOWN", safeReason,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}
