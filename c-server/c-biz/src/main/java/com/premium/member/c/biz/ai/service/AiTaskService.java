package com.premium.member.c.biz.ai.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.premium.member.c.biz.ai.dto.AiTaskCreateRequest;
import com.premium.member.c.biz.ai.dto.AiTaskResponse;
import com.premium.member.c.biz.ai.dto.AiTaskRow;
import com.premium.member.c.biz.ai.dto.AiTemplateResponse;
import com.premium.member.c.biz.ai.dto.QuotaResponse;
import com.premium.member.c.biz.ai.entity.AiTask;
import com.premium.member.c.biz.ai.entity.AiTemplate;
import com.premium.member.c.biz.ai.entity.Asset;
import com.premium.member.c.biz.ai.mapper.AiMapper;
import com.premium.member.c.biz.ai.storage.StorageService;
import com.premium.member.c.biz.benefit.entity.PmMemberPeriod;
import com.premium.member.c.biz.member.entity.PmMember;
import com.premium.member.c.biz.member.mapper.PmMemberMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * C 端 AI 模板、提交、查询与作品删除。
 */
@Service
public class AiTaskService {

    private static final DateTimeFormatter TASK_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> MOCK_RESULTS = Set.of("", "SUCCESS", "FAIL", "REJECT", "UNKNOWN");
    private static final Set<String> TYPES = Set.of("PHOTO", "DRESSUP", "VIDEO");

    private final AiMapper aiMapper;
    private final PmMemberMapper memberMapper;
    private final AiQuotaService quotaService;
    private final StorageService storageService;

    public AiTaskService(AiMapper aiMapper, PmMemberMapper memberMapper,
            AiQuotaService quotaService, StorageService storageService) {
        this.aiMapper = aiMapper;
        this.memberMapper = memberMapper;
        this.quotaService = quotaService;
        this.storageService = storageService;
    }

    public List<AiTemplateResponse> templates(String type, String category) {
        String normalizedType = StringUtils.hasText(type) ? type.trim().toUpperCase(Locale.ROOT) : "";
        if (StringUtils.hasText(normalizedType) && !TYPES.contains(normalizedType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "type 仅支持 PHOTO/DRESSUP/VIDEO");
        }
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : "";
        return aiMapper.selectTemplates(normalizedType, normalizedCategory).stream()
                .map(AiTemplateResponse::from)
                .toList();
    }

    @Transactional
    public QuotaResponse quota(String phone) {
        PmMember member = requireActiveMember(phone);
        refreshPeriods(member.getId());
        return quotaService.forPeriod(aiMapper.selectActivePeriod(member.getId()));
    }

    @Transactional
    public AiTaskResponse create(String phone, AiTaskCreateRequest request) {
        PmMember member = requireActiveMember(phone);
        String key = request.idempotencyKey().trim();
        String mockResult = normalizeMockResult(request.mockResult());
        AiTask existing = aiMapper.selectTaskByIdem(key);
        if (existing != null) {
            return existingResponse(existing, member.getId());
        }
        refreshPeriods(member.getId());
        PmMemberPeriod period = aiMapper.lockActivePeriod(member.getId());
        if (period == null) {
            throw new BusinessException(ErrorCode.AI_PERIOD_NOT_FOUND);
        }
        AiTemplate template = aiMapper.selectActiveTemplate(request.templateId());
        if (template == null) {
            throw new BusinessException(ErrorCode.AI_TEMPLATE_NOT_FOUND);
        }
        Asset input = requireInputAsset(request.inputAssetId(), member.getId());

        AiTask task = new AiTask();
        task.setTaskNo(newTaskNo());
        task.setMemberId(member.getId());
        task.setPeriodId(period.getId());
        task.setTemplateId(template.getId());
        task.setType(template.getType());
        task.setStatus("QUEUED");
        task.setInputAssetId(input.getId());
        task.setSupplierTaskNo("");
        task.setFailReason("");
        task.setCostCents(0L);
        task.setIdempotencyKey(key);
        task.setMockResult(mockResult);
        if (aiMapper.insertTask(task) == 0) {
            AiTask duplicate = aiMapper.selectTaskByIdem(key);
            if (duplicate == null) {
                throw new BusinessException(ErrorCode.CONFLICT);
            }
            return existingResponse(duplicate, member.getId());
        }
        quotaService.reserve(period.getId(), kind(task.getType()), task.getId(), task.getTaskNo(),
                member.getMemberNo());
        AiTaskRow row = aiMapper.selectTaskRow(task.getTaskNo(), member.getId());
        return row == null
                ? toResponse(task, template.getName(), input.getStorageKey(), null, null)
                : toResponse(row);
    }

    public List<AiTaskResponse> list(String phone, String status) {
        PmMember member = requireMember(phone);
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "";
        return aiMapper.selectTaskRows(member.getId(), normalized).stream()
                .map(this::toResponse)
                .toList();
    }

    public AiTaskResponse detail(String phone, String taskNo) {
        PmMember member = requireMember(phone);
        AiTaskRow row = aiMapper.selectTaskRow(taskNo, member.getId());
        if (row == null) {
            throw new BusinessException(ErrorCode.AI_TASK_NOT_FOUND);
        }
        return toResponse(row);
    }

    @Transactional
    public void deleteWork(String phone, long taskId) {
        PmMember member = requireMember(phone);
        AiTask task = aiMapper.selectTaskById(taskId);
        if (task == null || !member.getId().equals(task.getMemberId())
                || !"SUCCESS".equals(task.getStatus()) || task.getOutputAssetId() == null) {
            throw new BusinessException(ErrorCode.AI_WORK_NOT_FOUND);
        }
        Asset output = aiMapper.selectAsset(task.getOutputAssetId());
        if (output == null || !"ACTIVE".equals(output.getStatus())) {
            return;
        }
        if (aiMapper.markAssetDeleted(output.getId()) == 1) {
            storageService.delete(output.getStorageKey());
        }
    }

    private AiTaskResponse existingResponse(AiTask task, long memberId) {
        if (!Long.valueOf(memberId).equals(task.getMemberId())) {
            throw new BusinessException(ErrorCode.AI_IDEMPOTENCY_CONFLICT);
        }
        AiTaskRow row = aiMapper.selectTaskRow(task.getTaskNo(), memberId);
        return row == null ? toResponse(task, "", null, null, null) : toResponse(row);
    }

    private AiTaskResponse toResponse(AiTask task, String templateName, String inputKey,
            String outputKey, String outputMime) {
        return new AiTaskResponse(task.getId(), task.getTaskNo(), task.getPeriodId(),
                task.getTemplateId(), templateName, task.getType(), task.getStatus(),
                task.getInputAssetId(), url(inputKey), task.getOutputAssetId(), url(outputKey),
                outputMime, task.getFailReason(), task.getCostCents() == null ? 0 : task.getCostCents(),
                task.getCreatedAt(), task.getFinishedAt());
    }

    private AiTaskResponse toResponse(AiTaskRow row) {
        return new AiTaskResponse(row.getId(), row.getTaskNo(), row.getPeriodId(),
                row.getTemplateId(), row.getTemplateName(), row.getType(), row.getStatus(),
                row.getInputAssetId(), url(row.getInputStorageKey()), row.getOutputAssetId(),
                url(row.getOutputStorageKey()), row.getOutputMime(), row.getFailReason(),
                row.getCostCents() == null ? 0 : row.getCostCents(), row.getCreatedAt(),
                row.getFinishedAt());
    }

    private String url(String storageKey) {
        return StringUtils.hasText(storageKey) ? "/c/ai/files/" + storageKey : null;
    }

    private String normalizeMockResult(String value) {
        String result = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
        if (!MOCK_RESULTS.contains(result)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "mockResult 仅支持 SUCCESS/FAIL/REJECT/UNKNOWN");
        }
        return "SUCCESS".equals(result) ? "" : result;
    }

    private Asset requireInputAsset(long assetId, long memberId) {
        Asset asset = aiMapper.selectAsset(assetId);
        if (asset == null || !Long.valueOf(memberId).equals(asset.getOwnerMemberId())
                || !"INPUT".equals(asset.getPurpose()) || !"ACTIVE".equals(asset.getStatus())) {
            throw new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND);
        }
        if (asset.getExpireAt() != null && asset.getExpireAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND);
        }
        return asset;
    }

    private String kind(String type) {
        return "VIDEO".equals(type) ? "VIDEO" : "IMAGE";
    }

    private void refreshPeriods(long memberId) {
        aiMapper.expireEnded(memberId);
        aiMapper.activateDuePending(memberId);
    }

    private PmMember requireActiveMember(String phone) {
        PmMember member = requireMember(phone);
        if (!Integer.valueOf(1).equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBER_DISABLED);
        }
        return member;
    }

    private PmMember requireMember(String phone) {
        PmMember member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private String newTaskNo() {
        return "AI" + OffsetDateTime.now(ZoneOffset.UTC).format(TASK_TIME)
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
