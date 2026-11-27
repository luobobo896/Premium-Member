package com.premium.member.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.premium.member.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
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
}
