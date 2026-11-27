package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户启用角色码。
     *
     * @param userId 用户 ID
     * @return 角色码列表
     */
    @Select("""
            SELECT DISTINCT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            ORDER BY r.code
            """)
    List<String> selectCodesByUserId(@Param("userId") long userId);
}
