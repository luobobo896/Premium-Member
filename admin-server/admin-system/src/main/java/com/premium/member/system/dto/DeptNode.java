package com.premium.member.system.dto;

import java.util.List;

/**
 * 部门树节点。
 *
 * @param id       部门 ID
 * @param parentId 父部门 ID
 * @param name     部门名称
 * @param leader   负责人
 * @param phone    联系电话
 * @param sort     排序
 * @param status   状态：1 启用，0 停用
 * @param children 子部门
 */
public record DeptNode(
        Long id,
        Long parentId,
        String name,
        String leader,
        String phone,
        Integer sort,
        Integer status,
        List<DeptNode> children) {
}
