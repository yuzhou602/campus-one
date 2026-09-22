package com.campusone.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusone.application.dto.ApprovalAction;
import com.campusone.application.dto.ApplicationDTO;
import com.campusone.application.entity.ApprovalRecord;
import com.campusone.application.entity.ServiceApplication;

import java.util.List;

public interface ApprovalService {
    ServiceApplication submitApplication(ApplicationDTO dto, Long userId);
    List<ServiceApplication> getMyApprovals(Long userId, String status);
    IPage<ServiceApplication> getPendingApprovals(Long userId, int page, int pageSize);
    IPage<ServiceApplication> getProcessedApprovals(Long userId, int page, int pageSize);
    long countPendingApprovals(Long userId);
    List<ApprovalRecord> getApprovalTrail(Long applicationId);
    void processApproval(Long applicationId, Long approverId, ApprovalAction action);
    void rollbackApproval(Long applicationId, Long approverId, ApprovalAction dto);
    void urgeApplication(Long applicationId, Long userId);
    void initApprovalSteps(Long applicationId);
}
