package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询全部启用部门。
     *
     * @return 部门列表
     */
    @Select("SELECT * FROM sys_dept WHERE status = 1 AND deleted = 0 ORDER BY sort, id")
    List<SysDept> selectAllActive();

    /**
     * 查询全部未删除部门，管理端树包含停用节点。
     *
     * @return 部门列表
     */
    @Select("SELECT * FROM sys_dept WHERE deleted = 0 ORDER BY sort, id")
    List<SysDept> selectAll();

    /**
     * 统计直接子部门数。
     *
     * @param parentId 父部门 ID
     * @return 子部门数
     */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id = #{parentId} AND deleted = 0")
    long countChildren(@Param("parentId") long parentId);
}
