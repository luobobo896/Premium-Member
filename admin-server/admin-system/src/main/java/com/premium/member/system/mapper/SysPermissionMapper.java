package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户拥有的权限码。
     *
     * @param userId 用户 ID
     * @return 权限码列表
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE ur.user_id = #{userId} AND p.deleted = 0
            ORDER BY p.code
            """)
    List<String> selectCodesByUserId(@Param("userId") long userId);
}
