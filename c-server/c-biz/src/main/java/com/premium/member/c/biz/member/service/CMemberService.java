package com.premium.member.c.biz.member.service;

import com.premium.member.c.biz.member.dto.MemberMeResponse;
import com.premium.member.c.biz.member.entity.PmMember;
import com.premium.member.c.biz.member.mapper.PmMemberMapper;
import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * C 端会员信息查询。
 */
@Service
public class CMemberService {

    private final PmMemberMapper memberMapper;

    public CMemberService(PmMemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    public MemberMeResponse me(String phone) {
        // shortcut: 跨服务共享表，拆微服务时改为调用 admin API。
        PmMember member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return MemberMeResponse.from(member);
    }
}
