package com.premium.member.system.mapper;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.system.dto.OperationLogQuery;
import com.premium.member.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 分批删除指定时间之前的日志。
     *
     * @param cutoff 截止时间
     * @param limit  单批上限
     * @return 删除行数
     */
    @Delete("""
            DELETE FROM sys_operation_log
            WHERE id IN (
                SELECT id FROM sys_operation_log
                WHERE created_at < #{cutoff}
                ORDER BY created_at
                LIMIT #{limit}
            )
            """)
    int deleteBefore(@Param("cutoff") OffsetDateTime cutoff, @Param("limit") int limit);
}
