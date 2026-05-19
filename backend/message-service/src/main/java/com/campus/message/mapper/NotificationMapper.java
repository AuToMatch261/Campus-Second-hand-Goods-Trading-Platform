package com.campus.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.message.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Update("UPDATE t_notification SET read_at = NOW() "
            + "WHERE id = #{id} AND user_id = #{userId} AND read_at IS NULL AND deleted = 0")
    int markRead(@Param("userId") long userId, @Param("id") long id);

    @Update("UPDATE t_notification SET read_at = NOW() "
            + "WHERE user_id = #{userId} AND read_at IS NULL AND deleted = 0")
    int markAllRead(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM t_notification "
            + "WHERE user_id = #{userId} AND read_at IS NULL AND deleted = 0")
    long countUnread(@Param("userId") long userId);
}
