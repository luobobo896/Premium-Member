package com.premium.member.system.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.system.dto.DeptRequest;
import com.premium.member.system.dto.DeptNode;
import com.premium.member.system.entity.SysDept;
import com.premium.member.system.mapper.SysDeptMapper;
import com.premium.member.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public DeptService(SysDeptMapper deptMapper, SysUserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    /**
     * 返回启用部门树。
     *
     * @return 部门树
     */
    public List<DeptNode> tree() {
        List<SysDept> depts = deptMapper.selectAllActive();
        Set<Long> ids = depts.stream().map(SysDept::getId).collect(Collectors.toSet());
        Map<Long, List<SysDept>> children = depts.stream().collect(Collectors.groupingBy(
                dept -> dept.getParentId() == null ? 0L : dept.getParentId(),
                Collectors.mapping(Function.identity(), Collectors.toCollection(ArrayList::new))));
        List<SysDept> roots = depts.stream()
                .filter(dept -> dept.getParentId() == null || dept.getParentId() == 0
                        || !ids.contains(dept.getParentId()))
                .toList();
        return build(roots, children);
    }

    /**
     * 管理端部门树，包含停用节点，并支持名称与状态过滤（保留匹配节点的祖先）。
     *
     * @param name   名称模糊匹配，可空
     * @param status 状态过滤，可空
     * @return 部门树
     */
    public List<DeptNode> manageTree(String name, Integer status) {
        List<SysDept> all = deptMapper.selectAll();
        List<SysDept> filtered = filterWithAncestors(all, name, status);
        Set<Long> ids = filtered.stream().map(SysDept::getId).collect(Collectors.toSet());
        Map<Long, List<SysDept>> children = filtered.stream().collect(Collectors.groupingBy(
                dept -> dept.getParentId() == null ? 0L : dept.getParentId(),
                Collectors.mapping(Function.identity(), Collectors.toCollection(ArrayList::new))));
        List<SysDept> roots = filtered.stream()
                .filter(dept -> dept.getParentId() == null || dept.getParentId() == 0
                        || !ids.contains(dept.getParentId()))
                .toList();
        return build(roots, children);
    }

    /**
     * 创建部门。
     *
     * @param request 创建参数
     * @return 新部门
     */
    @Transactional
    public DeptNode create(DeptRequest request) {
        validateParent(request.parentId(), null);
        OffsetDateTime now = OffsetDateTime.now();
        SysDept dept = new SysDept();
        apply(dept, request);
        dept.setCreatedAt(now);
        dept.setUpdatedAt(now);
        dept.setDeleted(0);
        if (deptMapper.insert(dept) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "部门创建失败");
        }
        return toNode(dept);
    }

    /**
     * 编辑部门。
     *
     * @param id      部门 ID
     * @param request 编辑参数
     * @return 更新后的部门
     */
    @Transactional
    public DeptNode update(long id, DeptRequest request) {
        SysDept dept = requireDept(id);
        validateParent(request.parentId(), id);
        apply(dept, request);
        dept.setUpdatedAt(OffsetDateTime.now());
        if (deptMapper.updateById(dept) != 1) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
        return toNode(dept);
    }

    /**
     * 删除部门，存在子部门或用户时拒绝。
     *
     * @param id 部门 ID
     */
    @Transactional
    public void delete(long id) {
        requireDept(id);
        if (deptMapper.countChildren(id) > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
        }
        if (userMapper.countByDeptId(id) > 0) {
            throw new BusinessException(ErrorCode.DEPT_IN_USE);
        }
        if (deptMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
    }

    private List<DeptNode> build(List<SysDept> depts, Map<Long, List<SysDept>> children) {
        return depts.stream()
                .map(dept -> new DeptNode(
                        dept.getId(),
                        dept.getParentId(),
                        dept.getName(),
                        dept.getLeader(),
                        dept.getPhone(),
                        dept.getSort(),
                        dept.getStatus(),
                        build(children.getOrDefault(dept.getId(), List.of()), children)))
                .toList();
    }

    private List<SysDept> filterWithAncestors(List<SysDept> depts, String name, Integer status) {
        if (!StringUtils.hasText(name) && status == null) {
            return depts;
        }
        Map<Long, SysDept> byId = depts.stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
        Set<Long> keep = new HashSet<>();
        for (SysDept dept : depts) {
            boolean matches = (!StringUtils.hasText(name)
                    || dept.getName() != null && dept.getName().contains(name.trim()))
                    && (status == null || status.equals(dept.getStatus()));
            if (!matches) {
                continue;
            }
            SysDept cursor = dept;
            int guard = 0;
            while (cursor != null && guard++ < 100) {
                keep.add(cursor.getId());
                cursor = cursor.getParentId() == null || cursor.getParentId() == 0
                        ? null
                        : byId.get(cursor.getParentId());
            }
        }
        return depts.stream().filter(dept -> keep.contains(dept.getId())).toList();
    }

    private void apply(SysDept dept, DeptRequest request) {
        dept.setParentId(request.parentId() == null ? 0L : request.parentId());
        dept.setName(request.name().trim());
        dept.setLeader(request.leader() == null ? "" : request.leader().trim());
        dept.setPhone(request.phone() == null ? "" : request.phone().trim());
        dept.setSort(request.sort() == null ? 0 : request.sort());
        dept.setStatus(request.status() == null ? 1 : request.status());
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "父级不能是自身");
        }
        SysDept cursor = deptMapper.selectById(parentId);
        if (cursor == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND, "父级部门不存在");
        }
        int guard = 0;
        while (cursor != null && cursor.getParentId() != null && cursor.getParentId() != 0 && guard++ < 100) {
            if (Objects.equals(cursor.getId(), selfId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "父级不能是自身的子级");
            }
            cursor = deptMapper.selectById(cursor.getParentId());
        }
    }

    private SysDept requireDept(long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    private DeptNode toNode(SysDept dept) {
        return new DeptNode(dept.getId(), dept.getParentId(), dept.getName(), dept.getLeader(),
                dept.getPhone(), dept.getSort(), dept.getStatus(), List.of());
    }
}
