package com.premium.member.system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.premium.member.system.dto.DeptNode;
import com.premium.member.system.entity.SysDept;
import com.premium.member.system.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;

@Service
public class DeptService {

    private final SysDeptMapper deptMapper;

    public DeptService(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
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
}
