package com.premium.member.system.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.premium.member.framework.security.AuthenticatedUser;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.entity.SysDept;
import com.premium.member.system.entity.SysUser;
import com.premium.member.system.mapper.SysDeptMapper;
import com.premium.member.system.mapper.SysRoleMapper;
import com.premium.member.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
 * 按当前用户角色计算可见用户 ID，null 表示不受限。
 */
@Service
public class DataScopeService {

    private static final Map<String, Integer> SCOPE_RANK = Map.of(
            "ALL", 0, "DEPT_AND_CHILD", 1, "DEPT", 2, "SELF", 3);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public DataScopeService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
    }

    /**
     * 当前用户可见的用户 ID 集合。
     *
     * @return null 表示全部可见；否则为可见用户 ID
     */
    public Set<Long> visibleUserIds() {
        AuthenticatedUser current = SecurityUtils.currentUser();
        if (current.roles().contains("super_admin")) {
            return null;
        }
        SysUser user = userMapper.selectById(current.userId());
        if (user == null) {
            return Set.of(current.userId());
        }
        String scope = roleMapper.selectDataScopesByUserId(current.userId()).stream()
                .max(Comparator.comparingInt(item -> SCOPE_RANK.getOrDefault(item, 3)))
                .orElse("SELF");
        if (user.getDeptId() == null && !"ALL".equals(scope)) {
            scope = "SELF";
        }
        return switch (scope) {
            case "ALL" -> null;
            case "DEPT_AND_CHILD" -> new HashSet<>(userMapper.selectIdsByDeptIds(deptAndChildIds(user.getDeptId())));
            case "DEPT" -> new HashSet<>(userMapper.selectIdsByDeptIds(List.of(user.getDeptId())));
            default -> Set.of(current.userId());
        };
    }

    private List<Long> deptAndChildIds(long rootDeptId) {
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysDept dept : deptMapper.selectAll()) {
            children.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>()).add(dept.getId());
        }
        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootDeptId);
        while (!queue.isEmpty()) {
            Long deptId = queue.poll();
            result.add(deptId);
            children.getOrDefault(deptId, List.of()).forEach(queue::add);
        }
        return result;
    }
}
