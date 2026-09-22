package com.campusone.reservation.controller;

import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.reservation.dto.ReservationDTO;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.service.ReservationService;
import com.campusone.security.UserContext;
import com.campusone.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "场地预约")
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @Operation(summary = "创建预约")
    @PostMapping
    public ApiResponse<ResourceReservation> create(@Valid @RequestBody ReservationDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        ResourceReservation reservation = new ResourceReservation();
        reservation.setUserId(userId);
        reservation.setResourceId(dto.getResourceId());
        reservation.setReservationDate(dto.getReservationDate());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setPurpose(dto.getPurpose());
        reservation.setParticipantCount(dto.getAttendeeCount());
        return ApiResponse.success(reservationService.createReservation(reservation, userId));
    }

    @Operation(summary = "我的预约")
    @GetMapping("/my")
    public ApiResponse<PageResult<ResourceReservation>> myReservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(PageResult.of(reservationService.getMyReservations(userId, page, pageSize)));
    }

    @Operation(summary = "取消预约")
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        reservationService.cancelReservation(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public ApiResponse<ResourceReservation> getById(@PathVariable Long id) {
        ResourceReservation reservation = reservationService.getById(id);
        if (reservation == null) throw new BusinessException("预约不存在");
        Long userId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentUserRole();
        if (!userId.equals(reservation.getUserId())
                && !"ADMIN".equals(role)
                && !"SUPER_ADMIN".equals(role)) {
            throw new BusinessException(403, "无权查看该预约");
        }
        return ApiResponse.success(reservation);
    }

    @Operation(summary = "资源列表")
    @GetMapping("/resources")
    public ApiResponse<?> listResources(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(reservationService.listResourcesPage(buildingId, resourceType, page, pageSize));
    }

    @Operation(summary = "可用时段")
    @GetMapping("/availability")
    public ApiResponse<?> getAvailability(
            @RequestParam Long resourceId,
            @RequestParam String date) {
        return ApiResponse.success(reservationService.getAvailability(resourceId, date));
    }
}
