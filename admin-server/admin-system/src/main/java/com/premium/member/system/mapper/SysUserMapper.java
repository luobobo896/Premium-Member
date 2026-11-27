package com.premium.member.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按登录名查询未删除用户。
     *
     * @param username 登录名
     * @return 用户或 null
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 按 ID 查询未删除用户。
     *
     * @param id 用户 ID
     * @return 用户或 null
     */
    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted = 0")
    SysUser selectActiveById(@Param("id") long id);
}
