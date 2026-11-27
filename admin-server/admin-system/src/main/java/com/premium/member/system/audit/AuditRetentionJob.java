package com.premium.member.system.audit;

import java.time.OffsetDateTime;

import com.premium.member.system.mapper.SysOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日清理超过保留期的操作日志，不随启动立即执行。
 */
@Component
public class AuditRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);
    private static final int BATCH_SIZE = 1000;

    private final SysOperationLogMapper logMapper;

    @Value("${app.audit.retention-enabled:true}")
    private boolean enabled;

    @Value("${app.audit.retention-days:90}")
    private int retentionDays;

    public AuditRetentionJob(SysOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    /**
     * 按天分批删除过期日志。
     */
    @Scheduled(cron = "${app.audit.retention-cron:0 0 3 * * *}", zone = "Asia/Shanghai")
    public void cleanup() {
        if (!enabled) {
            log.debug("Audit retention cleanup disabled");
            return;
        }
        if (retentionDays <= 0) {
            log.warn("Audit retention days must be positive, got {}", retentionDays);
            return;
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        long total = 0;
        int deleted;
        do {
            deleted = logMapper.deleteBefore(cutoff, BATCH_SIZE);
            total += deleted;
        } while (deleted == BATCH_SIZE);
        log.info("Audit retention cleanup finished: deleted={} cutoff={}", total, cutoff);
    }
}
