package com.premium.member.c.biz.member.mapper;

import com.premium.member.c.biz.member.entity.PmMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PmMemberMapper {

    @Select("""
            SELECT id, member_no, username, nickname, phone, level, points, balance, status, joined_at
            FROM pm_member
            WHERE phone = #{phone} AND deleted = 0
            LIMIT 1
            """)
    PmMember selectByPhone(@Param("phone") String phone);
}
