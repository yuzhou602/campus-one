package com.campusone.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.repair.dto.RepairDTO;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.service.RepairService;
import com.campusone.security.UserContext;
import com.campusone.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "校园报修")
@RestController
@RequestMapping("/api/v1/repairs")
@RequiredArgsConstructor
public class RepairController {
    private final RepairService repairService;

    @Operation(summary = "提交报修")
    @PostMapping
    public ApiResponse<RepairOrder> create(@Valid @RequestBody RepairDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        RepairOrder order = new RepairOrder();
        order.setUserId(userId);
        order.setLocation(dto.getLocation());
        order.setCategory(dto.getCategory());
        order.setDescription(dto.getDescription());
        order.setContact(dto.getContact());
        order.setAvailableTime(dto.getAvailableTime());
        order.setPriority("MEDIUM");
        order.setStatus("SUBMITTED");
        order.setRepairNo("RP" + System.currentTimeMillis() + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        return ApiResponse.success(repairService.createRepair(order));
    }

    @Operation(summary = "我的报修")
    @GetMapping("/my")
    public ApiResponse<PageResult<RepairOrder>> myRepairs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(PageResult.of(repairService.getMyRepairs(userId, page, pageSize)));
    }

    @Operation(summary = "报修详情")
    @GetMapping("/{id}")
    public ApiResponse<RepairOrder> getById(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        RepairOrder order = repairService.getRepairById(id);
        if (order == null) {
            throw new com.campusone.common.exception.BusinessException("工单不存在");
        }
        String role = UserContext.getCurrentUserRole();
        boolean privileged = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "SERVICE".equals(role);
        boolean owner = userId.equals(order.getUserId());
        boolean assignee = userId.equals(order.getAssignedUserId());
        if (!privileged && !owner && !assignee) {
            throw new com.campusone.common.exception.BusinessException(403, "无权访问");
        }
        return ApiResponse.success(order);
    }

    @Operation(summary = "接单")
    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        repairService.acceptRepair(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "更新状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        Long userId = UserContext.getCurrentUserId();
        repairService.updateStatus(id, body.get("status"), userId);
        return ApiResponse.success();
    }

    @Operation(summary = "分配工单")
    @PostMapping("/{id}/assign")
    @RequiresRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Void> assign(@PathVariable Long id, @RequestBody java.util.Map<String, Long> body) {
        Long assigneeId = body.get("userId");
        if (assigneeId == null) {
            throw new com.campusone.common.exception.BusinessException("维修人员不能为空");
        }
        repairService.assignRepair(id, assigneeId);
        return ApiResponse.success();
    }

    @Operation(summary = "已分配工单")
    @GetMapping("/assigned")
    public ApiResponse<PageResult<RepairOrder>> assignedRepairs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        IPage<RepairOrder> result = repairService.getAssignedRepairs(userId, page, pageSize);
        return ApiResponse.success(PageResult.of(result));
    }
}
