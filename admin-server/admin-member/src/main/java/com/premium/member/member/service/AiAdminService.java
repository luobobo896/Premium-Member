package com.premium.member.member.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.member.dto.AiTaskQuery;
import com.premium.member.member.dto.AiTaskRow;
import com.premium.member.member.dto.AiTemplateRequest;
import com.premium.member.member.dto.AiTemplateResponse;
import com.premium.member.member.dto.AiUsageRow;
import com.premium.member.member.entity.AiTemplate;
import com.premium.member.member.mapper.AiAdminMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * B 端 AI 模板、任务与用量日报。
 */
@Service
public class AiAdminService {

    private final AiAdminMapper aiMapper;
    private final ObjectMapper objectMapper;

    public AiAdminService(AiAdminMapper aiMapper, ObjectMapper objectMapper) {
        this.aiMapper = aiMapper;
        this.objectMapper = objectMapper;
    }

    public List<AiTemplateResponse> templates() {
        return aiMapper.selectTemplates().stream().map(AiTemplateResponse::from).toList();
    }

    @Transactional
    public AiTemplateResponse create(AiTemplateRequest request) {
        AiTemplate template = new AiTemplate();
        template.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        template.setName(request.name().trim());
        template.setType(request.type());
        template.setCategory(value(request.category()));
        template.setCoverUrl(value(request.coverUrl()));
        template.setParams(params(request.params()));
        template.setStatus(request.status() == null ? 1 : request.status());
        template.setVersionNo(1);
        template.setSort(request.sort() == null ? 0 : request.sort());
        try {
            aiMapper.insertTemplate(template);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.AI_TEMPLATE_CODE_EXISTS);
        }
        return AiTemplateResponse.from(aiMapper.selectTemplate(template.getId()));
    }

    @Transactional
    public AiTemplateResponse update(long id, AiTemplateRequest request) {
        if (aiMapper.selectTemplate(id) == null) {
            throw new BusinessException(ErrorCode.AI_TEMPLATE_NOT_FOUND);
        }
        aiMapper.updateTemplate(id, request.name().trim(), request.type(),
                value(request.category()), value(request.coverUrl()), params(request.params()),
                request.status() == null ? 1 : request.status(),
                request.sort() == null ? 0 : request.sort());
        return AiTemplateResponse.from(aiMapper.selectTemplate(id));
    }

    @Transactional
    public void delete(long id) {
        if (aiMapper.deleteTemplate(id) != 1) {
            throw new BusinessException(ErrorCode.AI_TEMPLATE_NOT_FOUND);
        }
    }

    public PageResult<AiTaskRow> tasks(AiTaskQuery query) {
        Page<AiTaskRow> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<AiTaskRow> result = aiMapper.selectTaskPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public List<AiUsageRow> usage(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        return aiMapper.selectUsage(start, end);
    }

    private String params(String params) {
        if (!StringUtils.hasText(params)) {
            return "{}";
        }
        String value = params.trim();
        try {
            objectMapper.readTree(value);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "params 必须是合法 JSON");
        }
        return value;
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }
}
