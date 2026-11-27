package com.premium.member.member.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.member.dto.MemberCreateRequest;
import com.premium.member.member.dto.MemberQuery;
import com.premium.member.member.dto.MemberResponse;
import com.premium.member.member.dto.MemberUpdateRequest;
import com.premium.member.member.entity.PmMember;
import com.premium.member.member.mapper.MemberMapper;
import com.premium.member.system.service.DataScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberService {

    private static final DateTimeFormatter MEMBER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MemberMapper memberMapper;
    private final DataScopeService dataScopeService;

    public MemberService(MemberMapper memberMapper, DataScopeService dataScopeService) {
        this.memberMapper = memberMapper;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 分页查询会员。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<MemberResponse> page(MemberQuery query) {
        Page<PmMember> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<PmMember> result = memberMapper.selectPageByQuery(page, query, dataScopeService.visibleUserIds());
        return PageResult.of(result.getRecords().stream().map(MemberResponse::from).toList(), result.getTotal());
    }

    /**
     * 查询会员详情。
     *
     * @param id 会员 ID
     * @return 会员
     */
    public MemberResponse detail(long id) {
        return MemberResponse.from(requireVisibleMember(id));
    }

    /**
     * 创建会员。
     *
     * @param request 创建参数
     * @return 新会员
     */
    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        PmMember member = new PmMember();
        member.setMemberNo(nextMemberNo());
        member.setUsername(request.username().trim());
        member.setOwnerId(SecurityUtils.currentUser().userId());
        member.setNickname(valueOrDefault(request.nickname(), request.username().trim()));
        member.setPhone(valueOrEmpty(request.phone()));
        member.setLevel(valueOrDefault(request.level(), "BASIC").toUpperCase());
        member.setPoints(request.points() == null ? 0 : request.points());
        member.setBalance(request.balance() == null ? BigDecimal.ZERO : request.balance());
        member.setStatus(request.status() == null ? 1 : request.status());
        member.setJoinedAt(request.joinedAt() == null ? now : request.joinedAt());
        member.setRemark(valueOrEmpty(request.remark()));
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        member.setDeleted(0);
        if (memberMapper.insert(member) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "会员创建失败");
        }
        return detail(member.getId());
    }

    /**
     * 更新会员。
     *
     * @param id      会员 ID
     * @param request 更新参数
     * @return 更新后的会员
     */
    @Transactional
    public MemberResponse update(long id, MemberUpdateRequest request) {
        PmMember member = requireVisibleMember(id);
        member.setUsername(request.username().trim());
        member.setNickname(valueOrDefault(request.nickname(), request.username().trim()));
        member.setPhone(valueOrEmpty(request.phone()));
        member.setLevel(valueOrDefault(request.level(), "BASIC").toUpperCase());
        member.setPoints(request.points() == null ? 0 : request.points());
        member.setBalance(request.balance() == null ? BigDecimal.ZERO : request.balance());
        member.setStatus(request.status() == null ? 1 : request.status());
        member.setJoinedAt(request.joinedAt() == null ? member.getJoinedAt() : request.joinedAt());
        member.setRemark(valueOrEmpty(request.remark()));
        member.setUpdatedAt(OffsetDateTime.now());
        if (memberMapper.updateById(member) != 1) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return detail(id);
    }

    /**
     * 逻辑删除会员。
     *
     * @param id 会员 ID
     */
    @Transactional
    public void delete(long id) {
        requireVisibleMember(id);
        if (memberMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private PmMember requireVisibleMember(long id) {
        PmMember member = requireMember(id);
        Set<Long> visibleUserIds = dataScopeService.visibleUserIds();
        if (visibleUserIds != null
                && (member.getOwnerId() == null || !visibleUserIds.contains(member.getOwnerId()))) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private PmMember requireMember(long id) {
        PmMember member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private String nextMemberNo() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String memberNo = "M" + MEMBER_NO_DATE.format(OffsetDateTime.now())
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (!memberMapper.existsByMemberNo(memberNo)) {
                return memberNo;
            }
        }
        throw new BusinessException(ErrorCode.MEMBER_NO_EXISTS);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
