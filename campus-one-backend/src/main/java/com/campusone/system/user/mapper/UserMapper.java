package com.campusone.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusone.system.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT r.role_key FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<String> getUserRoles(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.role_key = #{role}")
    int hasUserRole(@Param("userId") Long userId, @Param("role") String role);

    @Select("""
            SELECT COUNT(DISTINCT p.id)
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_role r ON r.id = rp.role_id AND r.status = 1 AND r.deleted = 0
            JOIN sys_user u ON u.id = #{userId} AND u.deleted = 0
            WHERE p.perm_key = #{permission}
              AND (r.role_key = u.role OR EXISTS (
                    SELECT 1 FROM sys_user_role ur
                    WHERE ur.user_id = u.id AND ur.role_id = r.id
              ))
            """)
    int hasPermission(@Param("userId") Long userId, @Param("permission") String permission);

    @Select("""
            SELECT DISTINCT p.perm_key
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_role r ON r.id = rp.role_id AND r.status = 1 AND r.deleted = 0
            JOIN sys_user u ON u.id = #{userId} AND u.deleted = 0
            WHERE r.role_key = u.role OR EXISTS (
                SELECT 1 FROM sys_user_role ur
                WHERE ur.user_id = u.id AND ur.role_id = r.id
            )
            ORDER BY p.perm_key
            """)
    List<String> getUserPermissions(@Param("userId") Long userId);
}
