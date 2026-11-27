package com.premium.member.system.config;

import java.util.concurrent.Executor;

import com.premium.member.framework.audit.AuditQueueMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 审计日志异步线程池。队列满时丢弃日志，优先保证业务请求。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private final AuditQueueMetrics auditQueueMetrics;

    public AsyncConfig(AuditQueueMetrics auditQueueMetrics) {
        this.auditQueueMetrics = auditQueueMetrics;
    }

    @Bean("auditExecutor")
    public Executor auditExecutor(@Value("${app.audit.queue-capacity:500}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(Math.max(queueCapacity, 0));
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler((runnable, pool) -> auditQueueMetrics.recordDrop());
        executor.initialize();
        return executor;
    }
}
