package com.premium.member.c.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.c.user.entity.CUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CUserMapper extends BaseMapper<CUser> {

    @Select("SELECT * FROM c_user WHERE phone = #{phone} LIMIT 1")
    CUser selectByPhone(@Param("phone") String phone);

    @Select("SELECT EXISTS (SELECT 1 FROM c_user WHERE phone = #{phone})")
    boolean existsByPhone(@Param("phone") String phone);
}
