package com.campusone.reservation.controller;

import com.campusone.common.response.ApiResponse;
import com.campusone.common.exception.BusinessException;
import com.campusone.reservation.entity.CampusResource;
import com.campusone.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "场地资源")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {
    private final ReservationService reservationService;

    @Operation(summary = "场地列表")
    @GetMapping
    public ApiResponse<List<CampusResource>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long buildingId) {
        return ApiResponse.success(reservationService.listResources(type, buildingId));
    }

    @Operation(summary = "场地详情")
    @GetMapping("/{id}")
    public ApiResponse<CampusResource> getById(@PathVariable Long id) {
        CampusResource resource = reservationService.getResourceById(id);
        if (resource == null || !"AVAILABLE".equals(resource.getStatus())) {
            throw new BusinessException("场地不存在或不可预约");
        }
        return ApiResponse.success(resource);
    }

    @Operation(summary = "场地可用时段")
    @GetMapping("/{id}/availability")
    public ApiResponse<List<Map<String, Object>>> getAvailability(
            @PathVariable Long id, @RequestParam String date) {
        return ApiResponse.success(reservationService.getAvailability(id, date));
    }
}
