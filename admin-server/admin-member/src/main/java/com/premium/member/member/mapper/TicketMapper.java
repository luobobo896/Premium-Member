package com.premium.member.member.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.premium.member.member.dto.TicketQuery;
import com.premium.member.member.dto.TicketReplyRow;
import com.premium.member.member.dto.TicketRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketMapper {

    String SELECT_ROW = """
            SELECT t.id, t.ticket_no, t.user_type, t.member_id, m.member_no,
                   COALESCE(m.nickname, cu.nickname) AS member_name,
                   COALESCE(m.phone, cu.phone) AS phone,
                   t.user_id, t.order_no, t.category,
                   t.title, t.content, t.status, t.priority, t.assignee_admin_id,
                   u.real_name AS assignee_name, t.remark,
                   (SELECT count(*) FROM support_ticket_reply r WHERE r.ticket_id = t.id)
                       AS reply_count,
                   t.created_at, t.updated_at, t.resolved_at
            FROM support_ticket t
            LEFT JOIN pm_member m ON m.id = t.member_id
            LEFT JOIN c_user cu ON t.user_type = 'C' AND cu.id = t.user_id
            LEFT JOIN sys_user u ON u.id = t.assignee_admin_id
            """;

    @Select("""
            <script>
            """ + SELECT_ROW + """
            WHERE 1 = 1
            <if test="query.status != null and query.status != ''">
              AND t.status = #{query.status}
            </if>
            <if test="query.category != null and query.category != ''">
              AND t.category = #{query.category}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
              AND (t.ticket_no LIKE CONCAT('%', #{query.keyword}, '%')
                   OR t.order_no LIKE CONCAT('%', #{query.keyword}, '%')
                   OR t.title LIKE CONCAT('%', #{query.keyword}, '%')
                   OR m.phone LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            <if test="query.from != null">
              AND t.created_at &gt;= #{query.from}::date
            </if>
            <if test="query.to != null">
              AND t.created_at &lt; (#{query.to}::date + interval '1 day')
            </if>
            ORDER BY t.created_at DESC, t.id DESC
            </script>
            """)
    IPage<TicketRow> selectPage(Page<TicketRow> page, @Param("query") TicketQuery query);

    @Select(SELECT_ROW + " WHERE t.ticket_no = #{ticketNo}")
    TicketRow selectByNo(@Param("ticketNo") String ticketNo);

    @Select("""
            SELECT id, sender_type, sender_id, sender_name, content, created_at
            FROM support_ticket_reply
            WHERE ticket_id = #{ticketId}
            ORDER BY created_at, id
            """)
    List<TicketReplyRow> selectReplies(@Param("ticketId") long ticketId);

    @Insert("""
            INSERT INTO support_ticket_reply
                (ticket_id, sender_type, sender_id, sender_name, content, created_at)
            VALUES
                (#{ticketId}, #{senderType}, #{senderId}, #{senderName}, #{content}, now())
            """)
    int insertReply(@Param("ticketId") long ticketId, @Param("senderType") String senderType,
            @Param("senderId") Long senderId, @Param("senderName") String senderName,
            @Param("content") String content);

    @Update("""
            UPDATE support_ticket
            SET status = #{status}, priority = #{priority}, remark = #{remark},
                assignee_admin_id = #{assigneeAdminId}, resolved_at = #{resolvedAt},
                updated_at = now()
            WHERE ticket_no = #{ticketNo}
            """)
    int updateTicket(@Param("ticketNo") String ticketNo, @Param("status") String status,
            @Param("priority") String priority, @Param("remark") String remark,
            @Param("assigneeAdminId") Long assigneeAdminId,
            @Param("resolvedAt") OffsetDateTime resolvedAt);

    @Update("""
            UPDATE support_ticket
            SET status = 'PROCESSING', assignee_admin_id = #{adminId}, updated_at = now()
            WHERE ticket_no = #{ticketNo} AND status IN ('OPEN', 'PROCESSING')
            """)
    int accept(@Param("ticketNo") String ticketNo, @Param("adminId") long adminId);

    @Update("""
            UPDATE support_ticket
            SET status = CASE WHEN status = 'OPEN' THEN 'PROCESSING' ELSE status END,
                updated_at = now()
            WHERE ticket_no = #{ticketNo}
            """)
    int touchAfterReply(@Param("ticketNo") String ticketNo);
}
