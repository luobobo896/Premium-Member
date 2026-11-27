package com.premium.member.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.system.dto.OperationLogQuery;
import com.premium.member.system.dto.OperationLogResponse;
import com.premium.member.system.entity.SysOperationLog;
import com.premium.member.system.mapper.SysOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志写入与查询。
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final SysOperationLogMapper logMapper;

    public OperationLogService(SysOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    /**
     * 异步写入，失败只记应用日志，不影响主流程。
     *
     * @param operationLog 日志
     */
    @Async("auditExecutor")
    public void record(SysOperationLog operationLog) {
        try {
            logMapper.insert(operationLog);
        } catch (RuntimeException ex) {
            log.warn("Operation log write failed: {} {} {}", operationLog.getModule(),
                    operationLog.getAction(), operationLog.getPath(), ex);
        }
    }

    /**
     * 分页查询。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<OperationLogResponse> page(OperationLogQuery query) {
        Page<SysOperationLog> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<SysOperationLog> result = logMapper.selectPageByQuery(page, query);
        return PageResult.of(result.getRecords().stream().map(OperationLogResponse::from).toList(),
                result.getTotal());
    }
}
