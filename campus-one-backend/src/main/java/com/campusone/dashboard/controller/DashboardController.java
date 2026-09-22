package com.campusone.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusone.activity.entity.CampusActivity;
import com.campusone.activity.entity.ActivityRegistration;
import com.campusone.activity.mapper.ActivityRegistrationMapper;
import com.campusone.activity.mapper.CampusActivityMapper;
import com.campusone.common.response.ApiResponse;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.mapper.NotificationMapper;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.security.UserContext;
import com.campusone.security.RequiresRole;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final UserMapper userMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final ResourceReservationMapper reservationMapper;
    private final CampusActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final NotificationMapper notificationMapper;
    private final UserNotificationMapper userNotificationMapper;

    @Operation(summary = "学生仪表盘")
    @GetMapping("/student")
    public ApiResponse<Map<String, Object>> studentDashboard() {
        Long userId = UserContext.getCurrentUserId();
        Map<String, Object> data = new HashMap<>();

        long myRepairs = repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getUserId, userId));
        data.put("myRepairs", myRepairs);

        long pendingRepairs = repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getUserId, userId)
                .eq(RepairOrder::getStatus, "SUBMITTED"));
        data.put("pendingRepairs", pendingRepairs);

        long myReservations = reservationMapper.selectCount(
            new LambdaQueryWrapper<ResourceReservation>().eq(ResourceReservation::getUserId, userId));
        data.put("myReservations", myReservations);

        long upcoming = reservationMapper.selectCount(
            new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getUserId, userId)
                .eq(ResourceReservation::getStatus, "CONFIRMED")
                .ge(ResourceReservation::getReservationDate, java.time.LocalDate.now()));
        data.put("upcomingReservations", upcoming);

        long joinedActivities = registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>().eq(ActivityRegistration::getUserId, userId));
        data.put("joinedActivities", joinedActivities);

        long unreadNotices = userNotificationMapper.countUnread(userId);
        data.put("unreadNotices", unreadNotices);

        List<CampusActivity> recentActivities = activityMapper.selectList(
            new LambdaQueryWrapper<CampusActivity>()
                .eq(CampusActivity::getStatus, "ACTIVE")
                .orderByDesc(CampusActivity::getCreatedAt)
                .last("LIMIT 5"));
        data.put("recentActivities", recentActivities);

        return ApiResponse.success(data);
    }

    @Operation(summary = "教师仪表盘")
    @GetMapping("/teacher")
    public ApiResponse<Map<String, Object>> teacherDashboard() {
        Long userId = UserContext.getCurrentUserId();
        Map<String, Object> data = new HashMap<>();

        data.put("coursesCount", 0);

        long pendingApprovals = repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStatus, "SUBMITTED"));
        data.put("pendingApprovals", pendingApprovals);

        long myRepairs = repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getAssignedUserId, userId));
        data.put("assignedRepairs", myRepairs);

        long unreadNotices = notificationMapper.selectCount(
            new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTargetType, "ALL"));
        data.put("unreadNotices", unreadNotices);

        return ApiResponse.success(data);
    }

    @Operation(summary = "管理员仪表盘")
    @GetMapping("/admin")
    @RequiresRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Map<String, Object>> adminDashboard() {
        Map<String, Object> data = new HashMap<>();

        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        data.put("totalUsers", totalUsers);

        long totalRepairs = repairOrderMapper.selectCount(new LambdaQueryWrapper<>());
        data.put("totalRepairs", totalRepairs);

        long pendingRepairs = repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "SUBMITTED"));
        data.put("pendingRepairs", pendingRepairs);

        long totalReservations = reservationMapper.selectCount(new LambdaQueryWrapper<>());
        data.put("totalReservations", totalReservations);

        long todayReservations = reservationMapper.selectCount(
            new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getReservationDate, java.time.LocalDate.now()));
        data.put("todayReservations", todayReservations);

        long totalActivities = activityMapper.selectCount(new LambdaQueryWrapper<>());
        data.put("totalActivities", totalActivities);

        long activeActivities = activityMapper.selectCount(
            new LambdaQueryWrapper<CampusActivity>()
                .eq(CampusActivity::getStatus, "ACTIVE"));
        data.put("activeActivities", activeActivities);

        long totalRegistrations = registrationMapper.selectCount(new LambdaQueryWrapper<>());
        data.put("totalRegistrations", totalRegistrations);

        return ApiResponse.success(data);
    }
}
