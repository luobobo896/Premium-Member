package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询用户可访问的启用菜单，按钮也一并返回供权限树使用。
     *
     * @param userId 用户 ID
     * @return 菜单列表
     */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE ur.user_id = #{userId} AND m.status = 1 AND m.deleted = 0
            ORDER BY m.sort, m.id
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") long userId);

    /**
     * 查询全部启用菜单，供管理端菜单树使用。
     *
     * @return 菜单列表
     */
    @Select("SELECT * FROM sys_menu WHERE status = 1 AND deleted = 0 ORDER BY sort, id")
    List<SysMenu> selectAllActive();
}
