package com.premium.member.member.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.MemberQuery;
import com.premium.member.member.entity.PmMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

@Mapper
public interface MemberMapper extends BaseMapper<PmMember> {

    /**
     * 按业务条件分页查询，Wrapper 只允许出现在 Mapper 层。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    default IPage<PmMember> selectPageByQuery(Page<PmMember> page, MemberQuery query) {
        LambdaQueryWrapper<PmMember> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item
                    .like(PmMember::getMemberNo, keyword)
                    .or().like(PmMember::getUsername, keyword)
                    .or().like(PmMember::getNickname, keyword)
                    .or().like(PmMember::getPhone, keyword));
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(PmMember::getLevel, query.getLevel().trim().toUpperCase());
        }
        if (query.getStatus() != null) {
            wrapper.eq(PmMember::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(PmMember::getCreatedAt).orderByDesc(PmMember::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 按会员编号查询。
     *
     * @param memberNo 会员编号
     * @return 会员或 null
     */
    @Select("SELECT * FROM pm_member WHERE member_no = #{memberNo} AND deleted = 0 LIMIT 1")
    PmMember selectByMemberNo(@Param("memberNo") String memberNo);

    /**
     * 判断会员编号是否已存在。
     *
     * @param memberNo 会员编号
     * @return true 表示存在
     */
    @Select("SELECT EXISTS (SELECT 1 FROM pm_member WHERE member_no = #{memberNo} AND deleted = 0)")
    boolean existsByMemberNo(@Param("memberNo") String memberNo);
}
