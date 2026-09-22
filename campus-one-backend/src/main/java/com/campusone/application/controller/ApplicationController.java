package com.campusone.application.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusone.application.dto.ApprovalAction;
import com.campusone.application.dto.ApplicationDTO;
import com.campusone.application.entity.ServiceApplication;
import com.campusone.application.entity.ApprovalRecord;
import com.campusone.application.mapper.ServiceApplicationMapper;
import com.campusone.application.service.ApprovalService;
import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.security.UserContext;
import com.campusone.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "事务申请")
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApprovalService approvalService;
    private final ServiceApplicationMapper applicationMapper;

    @Operation(summary = "提交申请")
    @PostMapping
    public ApiResponse<ServiceApplication> submit(@RequestBody ApplicationDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(approvalService.submitApplication(dto, userId));
    }

    @Operation(summary = "我的申请")
    @GetMapping("/my")
    public ApiResponse<List<ServiceApplication>> myApplications(
            @RequestParam(required = false) String status) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(approvalService.getMyApprovals(userId, status));
    }

    @Operation(summary = "申请详情")
    @GetMapping("/{id}")
    public ApiResponse<ServiceApplication> getById(@PathVariable Long id) {
        ServiceApplication application = applicationMapper.selectById(id);
        authorizeAccess(application);
        return ApiResponse.success(application);
    }

    @Operation(summary = "待审批列表（按当前用户负责的卷宗）")
    @GetMapping("/approvals/pending")
    public ApiResponse<?> pendingApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(approvalService.getPendingApprovals(userId, page, pageSize));
    }

    @Operation(summary = "我审批过的列表")
    @GetMapping("/approvals/processed")
    public ApiResponse<?> processedApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(approvalService.getProcessedApprovals(userId, page, pageSize));
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approvals/{taskId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long taskId, @RequestBody ApprovalAction action) {
        Long userId = UserContext.getCurrentUserId();
        approvalService.processApproval(taskId, userId, action);
        return ApiResponse.success();
    }

    @Operation(summary = "待审批数量（当前用户）")
    @GetMapping("/approvals/pending/count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.success(approvalService.countPendingApprovals(UserContext.getCurrentUserId()));
    }

    @Operation(summary = "驳回")
    @PostMapping("/approvals/{taskId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long taskId, @RequestBody ApprovalAction action) {
        Long userId = UserContext.getCurrentUserId();
        action.setAction("REJECT");
        approvalService.processApproval(taskId, userId, action);
        return ApiResponse.success();
    }

    @Operation(summary = "审批意见留痕（按时间倒序）")
    @GetMapping("/{id}/approvals")
    public ApiResponse<List<ApprovalRecord>> approvalTrail(@PathVariable Long id) {
        authorizeAccess(applicationMapper.selectById(id));
        return ApiResponse.success(approvalService.getApprovalTrail(id));
    }

    @Operation(summary = "退回上一步")
    @PostMapping("/{id}/rollback")
    public ApiResponse<Void> rollback(@PathVariable Long id, @RequestBody ApprovalAction action) {
        Long userId = UserContext.getCurrentUserId();
        approvalService.rollbackApproval(id, userId, action);
        return ApiResponse.success();
    }

    @Operation(summary = "申请人催办")
    @PostMapping("/{id}/urge")
    public ApiResponse<Void> urge(@PathVariable Long id) {
        approvalService.urgeApplication(id, UserContext.getCurrentUserId());
        return ApiResponse.success();
    }

    private void authorizeAccess(ServiceApplication application) {
        if (application == null) throw new BusinessException("申请不存在");
        Long userId = UserContext.getCurrentUserId();
        String role = UserContext.getCurrentUserRole();
        boolean elevated = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        boolean applicant = userId.equals(application.getApplicantId());
        boolean assignee = approvalService.getApprovalTrail(application.getId()).stream()
                .anyMatch(record -> userId.equals(record.getAssigneeId()));
        if (!elevated && !applicant && !assignee) {
            throw new BusinessException(403, "无权查看该申请");
        }
    }
}
