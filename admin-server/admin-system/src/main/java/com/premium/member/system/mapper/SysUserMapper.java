package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.system.dto.UserQuery;
import com.premium.member.system.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

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

    /**
     * 按管理端条件分页查询用户。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    default IPage<SysUser> selectPageByQuery(Page<SysUser> page, UserQuery query, java.util.Set<Long> visibleUserIds) {
        var wrapper = Wrappers.lambdaQuery(SysUser.class);
        if (visibleUserIds != null) {
            if (visibleUserIds.isEmpty()) {
                wrapper.eq(SysUser::getId, -1L);
            } else {
                wrapper.in(SysUser::getId, visibleUserIds);
            }
        }
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername().trim());
        }
        if (StringUtils.hasText(query.getRealName())) {
            wrapper.like(SysUser::getRealName, query.getRealName().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, query.getDeptId());
        }
        wrapper.orderByAsc(SysUser::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 查询指定部门下的用户 ID。
     *
     * @param deptIds 部门 ID 集合，调用方保证非空
     * @return 用户 ID 列表
     */
    @Select("""
            <script>
            SELECT id FROM sys_user
            WHERE deleted = 0 AND dept_id IN
            <foreach collection="deptIds" item="deptId" open="(" separator="," close=")">#{deptId}</foreach>
            </script>
            """)
    List<Long> selectIdsByDeptIds(@Param("deptIds") List<Long> deptIds);

    /**
     * 判断登录名是否已存在。
     *
     * @param username 登录名
     * @return true 表示已存在
     */
    @Select("SELECT EXISTS (SELECT 1 FROM sys_user WHERE username = #{username} AND deleted = 0)")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 查询用户的角色 ID。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} ORDER BY role_id")
    List<Long> selectRoleIdsByUserId(@Param("userId") long userId);

    /**
     * 清空用户角色。
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteRolesByUserId(@Param("userId") long userId);

    /**
     * 批量写入用户角色。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表，调用方保证非空
     * @return 影响行数
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="roleIds" item="roleId" separator=",">(#{userId}, #{roleId})</foreach>
            </script>
            """)
    int insertUserRoles(@Param("userId") long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 统计部门下的用户数。
     *
     * @param deptId 部门 ID
     * @return 用户数
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND deleted = 0")
    long countByDeptId(@Param("deptId") long deptId);
}
