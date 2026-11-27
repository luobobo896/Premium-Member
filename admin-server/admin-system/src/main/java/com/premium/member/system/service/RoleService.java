package com.premium.member.system.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.cache.PermissionCache;
import com.premium.member.system.dto.PermissionResponse;
import com.premium.member.system.dto.RoleCreateRequest;
import com.premium.member.system.dto.RoleQuery;
import com.premium.member.system.dto.RoleResponse;
import com.premium.member.system.dto.RoleUpdateRequest;
import com.premium.member.system.entity.SysMenu;
import com.premium.member.system.entity.SysPermission;
import com.premium.member.system.entity.SysRole;
import com.premium.member.system.mapper.SysMenuMapper;
import com.premium.member.system.mapper.SysPermissionMapper;
import com.premium.member.system.mapper.SysRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统角色管理。
 */
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);
    private static final String SUPER_ADMIN = "super_admin";

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;
    private final PermissionCache permissionCache;

    public RoleService(SysRoleMapper roleMapper, SysMenuMapper menuMapper,
            SysPermissionMapper permissionMapper, PermissionCache permissionCache) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.permissionCache = permissionCache;
    }

    /**
     * 分页查询角色。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<RoleResponse> page(RoleQuery query) {
        Page<SysRole> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<SysRole> result = roleMapper.selectPageByQuery(page, query);
        return PageResult.of(result.getRecords().stream().map(RoleResponse::from).toList(), result.getTotal());
    }

    /**
     * 全部启用角色，供下拉选择。
     *
     * @return 角色列表
     */
    public List<RoleResponse> options() {
        return roleMapper.selectAllActive().stream().map(RoleResponse::from).toList();
    }

    /**
     * 创建角色。
     *
     * @param request 创建参数
     * @return 新角色
     */
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        String code = request.code().trim();
        if (roleMapper.existsByCode(code)) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        OffsetDateTime now = OffsetDateTime.now();
        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setStatus(request.status() == null ? 1 : request.status());
        role.setSort(request.sort() == null ? 0 : request.sort());
        role.setRemark(valueOrEmpty(request.remark()));
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        role.setDeleted(0);
        if (roleMapper.insert(role) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "角色创建失败");
        }
        return RoleResponse.from(role);
    }

    /**
     * 编辑角色，super_admin 角色码不可修改。
     *
     * @param id      角色 ID
     * @param request 编辑参数
     * @return 更新后的角色
     */
    @Transactional
    public RoleResponse update(long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        String code = request.code().trim();
        if (SUPER_ADMIN.equals(role.getCode()) && !SUPER_ADMIN.equals(code)) {
            throw new BusinessException(ErrorCode.ROLE_PROTECTED, "super_admin 角色码不能修改");
        }
        if (SUPER_ADMIN.equals(role.getCode()) && Integer.valueOf(0).equals(request.status())) {
            throw new BusinessException(ErrorCode.ROLE_PROTECTED, "super_admin 角色不能停用");
        }
        if (roleMapper.existsByCodeExcluding(code, id)) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        role.setCode(code);
        role.setName(request.name().trim());
        role.setStatus(request.status() == null ? role.getStatus() : request.status());
        role.setSort(request.sort() == null ? role.getSort() : request.sort());
        role.setRemark(valueOrEmpty(request.remark()));
        role.setUpdatedAt(OffsetDateTime.now());
        if (roleMapper.updateById(role) != 1) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        evictRoleUsers(id);
        return RoleResponse.from(role);
    }

    /**
     * 删除角色，super_admin 禁止删除。
     *
     * @param id 角色 ID
     */
    @Transactional
    public void delete(long id) {
        SysRole role = requireRole(id);
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.ROLE_PROTECTED);
        }
        if (roleMapper.countUsersByRoleId(id) > 0) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE);
        }
        evictRoleUsers(id);
        if (roleMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
    }

    /**
     * 查询角色已分配菜单 ID。
     *
     * @param id 角色 ID
     * @return 菜单 ID 列表
     */
    public List<Long> menuIds(long id) {
        requireRole(id);
        return roleMapper.selectMenuIdsByRoleId(id);
    }

    /**
     * 分配菜单，空列表表示清空。
     *
     * @param id      角色 ID
     * @param menuIds 菜单 ID 列表
     */
    @Transactional
    public void assignMenus(long id, List<Long> menuIds) {
        SysRole role = requireRole(id);
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.ROLE_PROTECTED, "super_admin 菜单不能修改");
        }
        List<Long> distinct = menuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            List<SysMenu> menus = menuMapper.selectBatchIds(distinct);
            if (menus.size() != distinct.size()) {
                throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
            }
        }
        roleMapper.deleteMenusByRoleId(id);
        if (!distinct.isEmpty()) {
            roleMapper.insertRoleMenus(id, distinct);
        }
        evictRoleUsers(id);
    }

    /**
     * 查询角色已分配权限点 ID。
     *
     * @param id 角色 ID
     * @return 权限 ID 列表
     */
    public List<Long> permissionIds(long id) {
        requireRole(id);
        return roleMapper.selectPermissionIdsByRoleId(id);
    }

    /**
     * 分配权限点，空列表表示清空。
     *
     * @param id            角色 ID
     * @param permissionIds 权限 ID 列表
     */
    @Transactional
    public void assignPermissions(long id, List<Long> permissionIds) {
        SysRole role = requireRole(id);
        if (SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.ROLE_PROTECTED, "super_admin 权限点不能修改");
        }
        List<Long> distinct = permissionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(distinct);
            if (permissions.size() != distinct.size()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "权限点不存在");
            }
        }
        roleMapper.deletePermissionsByRoleId(id);
        if (!distinct.isEmpty()) {
            roleMapper.insertRolePermissions(id, distinct);
        }
        evictRoleUsers(id);
    }

    /**
     * 全部权限点，供角色授权使用。
     *
     * @return 权限点列表
     */
    public List<PermissionResponse> permissions() {
        return permissionMapper.selectAll().stream().map(PermissionResponse::from).toList();
    }

    private SysRole requireRole(long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private void evictRoleUsers(long roleId) {
        try {
            roleMapper.selectUserIdsByRoleId(roleId).forEach(permissionCache::evict);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable, permission cache not evicted for role {}", roleId, ex);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
