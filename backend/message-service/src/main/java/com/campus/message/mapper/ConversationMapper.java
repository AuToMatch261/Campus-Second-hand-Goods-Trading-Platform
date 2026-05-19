package com.campus.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM t_conversation WHERE user_a = #{userA} AND user_b = #{userB} AND deleted = 0")
    Conversation findByPair(@Param("userA") long userA, @Param("userB") long userB);

    /** 写入最近消息预览 + 给接收方未读 +1（whichSide=A 表示 user_a 是接收方） */
    @Update("<script>"
            + "UPDATE t_conversation SET last_message_id = #{messageId}, "
            + "last_message_text = #{preview}, last_message_at = #{at}, updated_at = NOW(), "
            + "<choose>"
            + "  <when test='whichSide == \"A\"'>unread_for_a = unread_for_a + 1</when>"
            + "  <otherwise>unread_for_b = unread_for_b + 1</otherwise>"
            + "</choose>"
            + " WHERE id = #{id}"
            + "</script>")
    int applyNewMessage(@Param("id") long id,
                        @Param("messageId") long messageId,
                        @Param("preview") String preview,
                        @Param("at") LocalDateTime at,
                        @Param("whichSide") String whichSide);

    /** 把指定一方的未读清零 */
    @Update("<script>"
            + "UPDATE t_conversation SET "
            + "<choose>"
            + "  <when test='whichSide == \"A\"'>unread_for_a = 0</when>"
            + "  <otherwise>unread_for_b = 0</otherwise>"
            + "</choose>"
            + ", updated_at = NOW() WHERE id = #{id}"
            + "</script>")
    int clearUnread(@Param("id") long id, @Param("whichSide") String whichSide);

    @Select("SELECT COALESCE(SUM(CASE WHEN user_a = #{userId} THEN unread_for_a ELSE unread_for_b END), 0) "
            + "FROM t_conversation WHERE (user_a = #{userId} OR user_b = #{userId}) AND deleted = 0")
    long sumUnread(@Param("userId") long userId);
}
