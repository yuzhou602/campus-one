package com.campusone.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusone.notice.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
    @Insert("""
            INSERT IGNORE INTO user_notification (notification_id, user_id, is_read, created_at)
            SELECT #{notificationId}, id, 0, NOW()
            FROM sys_user
            WHERE status = 1
            """)
    int distributeToAllActiveUsers(@Param("notificationId") Long notificationId);

    @Update("UPDATE user_notification SET is_read = 1, read_at = NOW() WHERE notification_id = #{notifId} AND user_id = #{userId} AND is_read = 0")
    int markAsRead(@Param("notifId") Long notifId, @Param("userId") Long userId);

    @Update("UPDATE user_notification SET is_read = 1, read_at = NOW() WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);
}
