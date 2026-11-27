package com.premium.member.bootstrap.controller;

import java.time.OffsetDateTime;
import java.util.Map;

import com.premium.member.framework.audit.AuditQueueMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final AuditQueueMetrics auditQueueMetrics;

    @Value("${app.audit.queue-capacity:500}")
    private int auditQueueCapacity;

    public HealthController(AuditQueueMetrics auditQueueMetrics) {
        this.auditQueueMetrics = auditQueueMetrics;
    }

    /**
     * 返回 B 端服务存活状态，供探针和联调使用。
     *
     * @return 服务状态与当前时间
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "admin-server",
                "time", OffsetDateTime.now().toString(),
                "audit", Map.of(
                        "dropped", auditQueueMetrics.dropped(),
                        "queueCapacity", auditQueueCapacity));
    }
}
