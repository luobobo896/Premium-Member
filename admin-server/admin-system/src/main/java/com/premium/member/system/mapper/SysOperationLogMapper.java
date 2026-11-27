package com.premium.member.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.system.dto.OperationLogQuery;
import com.premium.member.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 按条件分页查询操作日志。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    default IPage<SysOperationLog> selectPageByQuery(Page<SysOperationLog> page, OperationLogQuery query) {
        var wrapper = Wrappers.lambdaQuery(SysOperationLog.class);
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysOperationLog::getUsername, query.getUsername().trim());
        }
        if (StringUtils.hasText(query.getModule())) {
            wrapper.eq(SysOperationLog::getModule, query.getModule().trim());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SysOperationLog::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SysOperationLog::getCreatedAt, query.getEndTime());
        }
        wrapper.orderByDesc(SysOperationLog::getCreatedAt).orderByDesc(SysOperationLog::getId);
        return selectPage(page, wrapper);
    }
}
