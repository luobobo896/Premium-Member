package com.premium.member.member.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员。
 */
@Data
@TableName("pm_member")
public class PmMember {

    /** 会员主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员编号，未删除会员内唯一。 */
    private String memberNo;

    /** 登录名。 */
    private String username;

    /** 归属用户 ID，数据权限按此过滤。 */
    private Long ownerId;

    /** 昵称。 */
    private String nickname;

    /** 手机号。 */
    private String phone;

    /** 等级：BASIC/SILVER/GOLD/PLATINUM。 */
    private String level;

    /** 积分，不能为负。 */
    private Integer points;

    /** 余额，单位元，保留两位小数。 */
    private BigDecimal balance;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 入会时间。 */
    private OffsetDateTime joinedAt;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private OffsetDateTime createdAt;

    /** 更新时间。 */
    private OffsetDateTime updatedAt;

    /** 逻辑删除：1 已删除，0 未删除。 */
    @TableLogic
    private Integer deleted;
}
