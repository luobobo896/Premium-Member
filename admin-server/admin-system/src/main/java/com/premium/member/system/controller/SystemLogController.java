package com.premium.member.system.controller;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.dto.OperationLogQuery;
import com.premium.member.system.dto.OperationLogResponse;
import com.premium.member.system.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询，仅超级管理员。
 */
@RestController
@RequestMapping("/system/logs")
public class SystemLogController {

    private final OperationLogService operationLogService;

    public SystemLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志。
     *
     * @param query page/pageSize/username/module/startTime/endTime
     * @return 分页结果
     */
    @GetMapping
    public R<PageResult<OperationLogResponse>> list(@Valid OperationLogQuery query) {
        if (!SecurityUtils.currentUser().roles().contains("super_admin")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可查看操作日志");
        }
        return R.ok(operationLogService.page(query));
    }
}
