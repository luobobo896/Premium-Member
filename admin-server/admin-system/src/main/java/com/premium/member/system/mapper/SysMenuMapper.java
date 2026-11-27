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

    /**
     * 查询全部未删除菜单，管理端树包含停用节点。
     *
     * @return 菜单列表
     */
    @Select("SELECT * FROM sys_menu WHERE deleted = 0 ORDER BY sort, id")
    List<SysMenu> selectAll();

    /**
     * 统计直接子菜单数。
     *
     * @param parentId 父菜单 ID
     * @return 子菜单数
     */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId} AND deleted = 0")
    long countChildren(@Param("parentId") long parentId);

    /**
     * 统计引用该菜单的角色数。
     *
     * @param menuId 菜单 ID
     * @return 引用数
     */
    @Select("SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = #{menuId}")
    long countRoleRefs(@Param("menuId") long menuId);
}
