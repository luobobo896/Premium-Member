package com.premium.member.c.biz.ticket.mapper;

import java.util.List;

import com.premium.member.c.biz.ticket.dto.CTicketReplyView;
import com.premium.member.c.biz.ticket.dto.CTicketView;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CTicketMapper {

    @Select("SELECT id FROM pm_member WHERE phone = #{phone} AND deleted = 0 LIMIT 1")
    Long selectMemberIdByPhone(@Param("phone") String phone);

    @Insert("""
            INSERT INTO support_ticket
                (ticket_no, user_type, member_id, user_id, order_no, category, title, content,
                 status, priority, created_at, updated_at)
            VALUES
                (#{ticketNo}, 'C', #{memberId}, #{userId}, #{orderNo}, #{category}, #{title},
                 #{content}, 'OPEN', #{priority}, now(), now())
            """)
    int insert(@Param("ticketNo") String ticketNo, @Param("memberId") Long memberId,
            @Param("userId") long userId, @Param("orderNo") String orderNo,
            @Param("category") String category, @Param("title") String title,
            @Param("content") String content, @Param("priority") String priority);

    @Select("""
            SELECT t.id, t.ticket_no, t.order_no, t.category, t.title, t.content, t.status,
                   t.priority, t.created_at, t.updated_at, t.resolved_at,
                   (SELECT count(*) FROM support_ticket_reply r WHERE r.ticket_id = t.id)
                       AS reply_count
            FROM support_ticket t
            WHERE t.user_type = 'C' AND t.user_id = #{userId}
            ORDER BY t.created_at DESC, t.id DESC
            LIMIT 100
            """)
    List<CTicketView> selectMine(@Param("userId") long userId);

    @Select("""
            SELECT t.id, t.ticket_no, t.order_no, t.category, t.title, t.content, t.status,
                   t.priority, t.created_at, t.updated_at, t.resolved_at,
                   (SELECT count(*) FROM support_ticket_reply r WHERE r.ticket_id = t.id)
                       AS reply_count
            FROM support_ticket t
            WHERE t.ticket_no = #{ticketNo} AND t.user_type = 'C' AND t.user_id = #{userId}
            """)
    CTicketView selectMineByNo(@Param("ticketNo") String ticketNo, @Param("userId") long userId);

    @Select("""
            SELECT sender_type, sender_name, content, created_at
            FROM support_ticket_reply
            WHERE ticket_id = #{ticketId}
              AND sender_type = 'B'
            ORDER BY created_at, id
            """)
    List<CTicketReplyView> selectAdminReplies(@Param("ticketId") long ticketId);
}
