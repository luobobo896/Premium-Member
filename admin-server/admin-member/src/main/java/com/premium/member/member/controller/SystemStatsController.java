package com.premium.member.member.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.member.dto.DailyStatRow;
import com.premium.member.member.service.StatsAdminService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * B 端每日统计与 CSV 导出。
 */
@RestController
@RequestMapping("/system/stats")
public class SystemStatsController {

    private final StatsAdminService statsService;

    public SystemStatsController(StatsAdminService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('stats:daily')")
    public R<List<DailyStatRow>> daily(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String source) {
        return R.ok(statsService.daily(from, to, source));
    }

    @GetMapping("/export")
    @AuditLog(module = "运营统计", action = "导出 CSV")
    @PreAuthorize("hasAuthority('stats:export')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String source) {
        byte[] body = statsService.csv(from, to, source).getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        String filename = "stats-" + start + "-" + end + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(withBom);
    }
}
