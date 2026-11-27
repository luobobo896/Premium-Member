package com.premium.member.member.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.member.dto.DailyStatRow;
import com.premium.member.member.mapper.StatsMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 每日运营统计与 CSV 导出。
 */
@Service
public class StatsAdminService {

    private static final int MAX_EXPORT_ROWS = 50_000;

    private final StatsMapper statsMapper;

    public StatsAdminService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    public List<DailyStatRow> daily(LocalDate from, LocalDate to, String source) {
        Range range = range(from, to);
        return statsMapper.selectDaily(range.from(), range.to(), normalizeSource(source));
    }

    public String csv(LocalDate from, LocalDate to, String source) {
        Range range = range(from, to);
        List<DailyStatRow> rows = statsMapper.selectDaily(range.from(), range.to(),
                normalizeSource(source));
        if (rows.size() > MAX_EXPORT_ROWS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "单次导出不能超过 5 万行");
        }
        StringBuilder csv = new StringBuilder(
                "日期,订单数,实付金额(分),有效会员数,权益申请,权益成功,权益异常,AI图片,AI视频\n");
        for (DailyStatRow row : rows) {
            csv.append(row.getDay()).append(',')
                    .append(value(row.getOrderCount())).append(',')
                    .append(value(row.getPaidAmountCents())).append(',')
                    .append(value(row.getActiveMembers())).append(',')
                    .append(value(row.getBenefitApplied())).append(',')
                    .append(value(row.getBenefitSuccess())).append(',')
                    .append(value(row.getBenefitException())).append(',')
                    .append(value(row.getAiImageTasks())).append(',')
                    .append(value(row.getAiVideoTasks())).append('\n');
        }
        return csv.toString();
    }

    private Range range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        return new Range(start, end);
    }

    private String normalizeSource(String source) {
        return StringUtils.hasText(source)
                ? source.trim().toUpperCase(Locale.ROOT) : null;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private record Range(LocalDate from, LocalDate to) {
    }
}
