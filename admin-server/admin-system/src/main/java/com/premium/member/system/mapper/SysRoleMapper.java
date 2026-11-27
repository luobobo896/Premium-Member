package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.system.dto.RoleQuery;
import com.premium.member.system.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

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

    /**
     * 按管理端条件分页查询角色。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    default IPage<SysRole> selectPageByQuery(Page<SysRole> page, RoleQuery query) {
        var wrapper = Wrappers.lambdaQuery(SysRole.class);
        if (StringUtils.hasText(query.getCode())) {
            wrapper.like(SysRole::getCode, query.getCode().trim());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(SysRole::getName, query.getName().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 查询全部启用角色，供下拉选择。
     *
     * @return 角色列表
     */
    @Select("SELECT * FROM sys_role WHERE status = 1 AND deleted = 0 ORDER BY sort, id")
    List<SysRole> selectAllActive();

    /**
     * 判断角色码是否已存在。
     *
     * @param code 角色码
     * @return true 表示已存在
     */
    @Select("SELECT EXISTS (SELECT 1 FROM sys_role WHERE code = #{code} AND deleted = 0)")
    boolean existsByCode(@Param("code") String code);

    /**
     * 判断角色码是否已被其他角色占用。
     *
     * @param code      角色码
     * @param excludeId 排除的角色 ID
     * @return true 表示已存在
     */
    @Select("""
            SELECT EXISTS (
                SELECT 1 FROM sys_role WHERE code = #{code} AND deleted = 0 AND id <> #{excludeId}
            )
            """)
    boolean existsByCodeExcluding(@Param("code") String code, @Param("excludeId") long excludeId);

    /**
     * 查询角色已分配菜单 ID。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId} ORDER BY menu_id")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") long roleId);

    /**
     * 清空角色菜单。
     *
     * @param roleId 角色 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteMenusByRoleId(@Param("roleId") long roleId);

    /**
     * 批量写入角色菜单。
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表，调用方保证非空
     * @return 影响行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="menuIds" item="menuId" separator=",">(#{roleId}, #{menuId})</foreach>
            </script>
            """)
    int insertRoleMenus(@Param("roleId") long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 查询角色已分配权限点 ID。
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId} ORDER BY permission_id")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") long roleId);

    /**
     * 清空角色权限点。
     *
     * @param roleId 角色 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deletePermissionsByRoleId(@Param("roleId") long roleId);

    /**
     * 批量写入角色权限点。
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表，调用方保证非空
     * @return 影响行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id) VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">(#{roleId}, #{permissionId})</foreach>
            </script>
            """)
    int insertRolePermissions(@Param("roleId") long roleId, @Param("permissionIds") List<Long> permissionIds);

    /**
     * 查询绑定了该角色的用户 ID。
     *
     * @param roleId 角色 ID
     * @return 用户 ID 列表
     */
    @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") long roleId);

    /**
     * 统计绑定了该角色的用户数。
     *
     * @param roleId 角色 ID
     * @return 用户数
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    long countUsersByRoleId(@Param("roleId") long roleId);

}
