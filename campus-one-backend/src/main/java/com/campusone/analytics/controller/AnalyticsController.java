package com.campusone.analytics.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusone.activity.entity.CampusActivity;
import com.campusone.activity.entity.ActivityRegistration;
import com.campusone.activity.mapper.ActivityRegistrationMapper;
import com.campusone.activity.mapper.CampusActivityMapper;
import com.campusone.common.response.ApiResponse;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.system.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.campusone.security.RequiresRole;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "数据分析")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@RequiresRole({"ADMIN", "SUPER_ADMIN"})
public class AnalyticsController {
    private final UserMapper userMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final ResourceReservationMapper reservationMapper;
    private final CampusActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;

    @Operation(summary = "运营数据概览")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("totalRepairs", repairOrderMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("pendingRepairs", repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "SUBMITTED")));
        data.put("totalReservations", reservationMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("todayReservations", reservationMapper.selectCount(
            new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getReservationDate, LocalDate.now())));
        data.put("totalActivities", activityMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("totalRegistrations", registrationMapper.selectCount(new LambdaQueryWrapper<>()));
        return ApiResponse.success(data);
    }

    @Operation(summary = "场地利用分析")
    @GetMapping("/venue")
    public ApiResponse<Map<String, Object>> venueAnalytics() {
        Map<String, Object> data = new HashMap<>();
        long total = reservationMapper.selectCount(new LambdaQueryWrapper<>());
        long today = reservationMapper.selectCount(
            new LambdaQueryWrapper<ResourceReservation>()
                .eq(ResourceReservation::getReservationDate, LocalDate.now()));
        data.put("totalReservations", total);
        data.put("todayReservations", today);
        data.put("avgDaily", total > 0 ? total / 30 : 0);
        return ApiResponse.success(data);
    }

    @Operation(summary = "报修分析")
    @GetMapping("/repair")
    public ApiResponse<Map<String, Object>> repairAnalytics() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalOrders", repairOrderMapper.selectCount(new LambdaQueryWrapper<>()));
        data.put("submitted", repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "SUBMITTED")));
        data.put("accepted", repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "ACCEPTED")));
        data.put("resolved", repairOrderMapper.selectCount(
            new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getStatus, "RESOLVED")));
        return ApiResponse.success(data);
    }
}
