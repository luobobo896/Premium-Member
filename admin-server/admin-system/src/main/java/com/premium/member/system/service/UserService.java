package com.premium.member.system.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.common.api.PageResult;
import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.cache.PermissionCache;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.dto.UserCreateRequest;
import com.premium.member.system.dto.UserQuery;
import com.premium.member.system.dto.UserResponse;
import com.premium.member.system.dto.UserUpdateRequest;
import com.premium.member.system.entity.SysDept;
import com.premium.member.system.entity.SysRole;
import com.premium.member.system.entity.SysUser;
import com.premium.member.system.mapper.SysDeptMapper;
import com.premium.member.system.mapper.SysRoleMapper;
import com.premium.member.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 系统用户管理。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String BUILTIN_ADMIN = "admin";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionCache permissionCache;
    private final DataScopeService dataScopeService;

    public UserService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper,
            PasswordEncoder passwordEncoder, PermissionCache permissionCache, DataScopeService dataScopeService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
        this.passwordEncoder = passwordEncoder;
        this.permissionCache = permissionCache;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 分页查询用户。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public PageResult<UserResponse> page(UserQuery query) {
        Page<SysUser> page = new Page<>(query.getPage(), query.getPageSize());
        page.setOptimizeCountSql(false);
        IPage<SysUser> result = userMapper.selectPageByQuery(page, query, dataScopeService.visibleUserIds());
        Map<Long, String> deptNames = deptMapper.selectAll().stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
        List<UserResponse> items = result.getRecords().stream()
                .map(user -> UserResponse.from(user, deptNames.get(user.getDeptId()),
                        userMapper.selectRoleIdsByUserId(user.getId())))
                .toList();
        return PageResult.of(items, result.getTotal());
    }

    /**
     * 创建用户。
     *
     * @param request 创建参数
     * @return 新用户
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        if (userMapper.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        requireDept(request.deptId());
        OffsetDateTime now = OffsetDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setEmail(valueOrNull(request.email()));
        user.setPhone(valueOrNull(request.phone()));
        user.setDeptId(request.deptId());
        user.setStatus(request.status() == null ? 1 : request.status());
        user.setDescription(valueOrEmpty(request.description()));
        user.setAvatar("");
        user.setHomePath("/analytics");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        if (userMapper.insert(user) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户创建失败");
        }
        return detail(user.getId());
    }

    /**
     * 编辑用户基础信息。
     *
     * @param id      用户 ID
     * @param request 编辑参数
     * @return 更新后的用户
     */
    @Transactional
    public UserResponse update(long id, UserUpdateRequest request) {
        SysUser user = requireVisibleUser(id);
        requireDept(request.deptId());
        int status = request.status() == null ? user.getStatus() : request.status();
        if (status == 0) {
            ensureDisableAllowed(user);
        }
        user.setRealName(request.realName().trim());
        user.setEmail(valueOrNull(request.email()));
        user.setPhone(valueOrNull(request.phone()));
        user.setDeptId(request.deptId());
        user.setStatus(status);
        user.setDescription(valueOrEmpty(request.description()));
        user.setUpdatedAt(OffsetDateTime.now());
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return detail(id);
    }

    /**
     * 删除用户，内置 admin 与当前登录用户禁止删除。
     *
     * @param id 用户 ID
     */
    @Transactional
    public void delete(long id) {
        SysUser user = requireVisibleUser(id);
        if (isBuiltinAdmin(user)) {
            throw new BusinessException(ErrorCode.USER_PROTECTED, "内置 admin 用户不能删除");
        }
        if (SecurityUtils.currentUser().userId() == id) {
            throw new BusinessException(ErrorCode.USER_PROTECTED, "不能删除当前登录用户");
        }
        if (userMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 重置密码。
     *
     * @param id       用户 ID
     * @param password 新密码
     */
    @Transactional
    public void resetPassword(long id, String password) {
        SysUser user = requireVisibleUser(id);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUpdatedAt(OffsetDateTime.now());
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 启用/停用用户，内置 admin 与当前登录用户禁止停用。
     *
     * @param id     用户 ID
     * @param status 状态：1 启用，0 停用
     */
    @Transactional
    public void updateStatus(long id, int status) {
        SysUser user = requireVisibleUser(id);
        if (status == 0) {
            ensureDisableAllowed(user);
        }
        user.setStatus(status);
        user.setUpdatedAt(OffsetDateTime.now());
        if (userMapper.updateById(user) != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 分配角色，空列表表示清空。
     *
     * @param id      用户 ID
     * @param roleIds 角色 ID 列表
     */
    @Transactional
    public void assignRoles(long id, List<Long> roleIds) {
        SysUser user = requireVisibleUser(id);
        if (isBuiltinAdmin(user)) {
            throw new BusinessException(ErrorCode.USER_PROTECTED, "内置 admin 角色不能修改");
        }
        List<Long> distinct = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(distinct);
            if (roles.size() != distinct.size()) {
                throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
            }
        }
        userMapper.deleteRolesByUserId(id);
        if (!distinct.isEmpty()) {
            userMapper.insertUserRoles(id, distinct);
        }
        evictPermissionCache(id);
    }

    private UserResponse detail(long id) {
        SysUser user = requireUser(id);
        SysDept dept = user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
        return UserResponse.from(user, dept == null ? null : dept.getName(),
                userMapper.selectRoleIdsByUserId(id));
    }

    private SysUser requireUser(long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private SysUser requireVisibleUser(long id) {
        SysUser user = requireUser(id);
        Set<Long> visibleUserIds = dataScopeService.visibleUserIds();
        if (visibleUserIds != null && !visibleUserIds.contains(id)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void requireDept(Long deptId) {
        if (deptId != null && deptMapper.selectById(deptId) == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
    }

    private void ensureDisableAllowed(SysUser user) {
        if (isBuiltinAdmin(user)) {
            throw new BusinessException(ErrorCode.USER_PROTECTED, "内置 admin 用户不能停用");
        }
        if (SecurityUtils.currentUser().userId() == user.getId()) {
            throw new BusinessException(ErrorCode.USER_PROTECTED, "不能停用当前登录用户");
        }
    }

    private boolean isBuiltinAdmin(SysUser user) {
        return user.getId() == 1L || BUILTIN_ADMIN.equals(user.getUsername());
    }

    private void evictPermissionCache(long userId) {
        try {
            permissionCache.evict(userId);
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable, permission cache not evicted for user {}", userId, ex);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
