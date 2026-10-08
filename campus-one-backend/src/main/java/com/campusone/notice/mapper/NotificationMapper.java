package com.campusone.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.notice.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    @Select("""
            <script>
            SELECT n.id, n.title, n.content, n.type, n.target_type, n.target_id,
                   n.sender_id, n.created_at,
                   CASE WHEN un.is_read = 1 THEN 1 ELSE 0 END AS is_read
            FROM user_notification un
            INNER JOIN sys_notification n ON n.id = un.notification_id
            WHERE un.user_id = #{userId}
            <if test="type != null and type != '' and type != 'all'">
              AND LOWER(n.type) = LOWER(#{type})
            </if>
            ORDER BY un.created_at DESC, un.id DESC
            </script>
            """)
    IPage<Notification> selectUserNotificationPage(
            Page<Notification> page,
            @Param("userId") Long userId,
            @Param("type") String type);
}
