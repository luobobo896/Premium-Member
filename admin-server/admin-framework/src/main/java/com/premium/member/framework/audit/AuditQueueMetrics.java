package com.premium.member.framework.audit;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审计队列丢弃计数，并定期 WARN 汇报增量。
 */
@Component
public class AuditQueueMetrics {

    private static final Logger log = LoggerFactory.getLogger(AuditQueueMetrics.class);

    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong reported = new AtomicLong();

    /**
     * 队列满丢弃一条日志。
     */
    public void recordDrop() {
        dropped.incrementAndGet();
    }

    public long dropped() {
        return dropped.get();
    }

    /**
     * 每分钟汇报自上次以来丢弃的数量。
     */
    @Scheduled(fixedDelayString = "${app.audit.drop-report-interval-ms:60000}")
    public void reportDropped() {
        long total = dropped.get();
        long delta = total - reported.getAndSet(total);
        if (delta > 0) {
            log.warn("Audit queue full, dropped {} operation logs (total {})", delta, total);
        }
    }
}
