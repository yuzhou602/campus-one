package com.campusone.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.application.dto.ApprovalAction;
import com.campusone.application.dto.ApplicationDTO;
import com.campusone.application.entity.ApprovalRecord;
import com.campusone.application.entity.ServiceApplication;
import com.campusone.application.mapper.ApprovalRecordMapper;
import com.campusone.application.mapper.ServiceApplicationMapper;
import com.campusone.application.service.ApprovalService;
import com.campusone.application.support.ServiceCatalogRegistry;
import com.campusone.common.exception.BusinessException;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {
    private final ServiceApplicationMapper applicationMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public ServiceApplication submitApplication(ApplicationDTO dto, Long userId) {
        ServiceApplication app = new ServiceApplication();
        app.setApplicantId(userId);
        app.setServiceId(dto.getServiceId());
        app.setFormDataJson(dto.getFormData());
        app.setStatus("PENDING");
        app.setApplicationNo("APP" + System.currentTimeMillis());
        app.setSubmittedAt(LocalDateTime.now());
        applicationMapper.insert(app);
        initApprovalSteps(app.getId());
        return app;
    }

    @Override
    public List<ServiceApplication> getMyApprovals(Long userId, String status) {
        LambdaQueryWrapper<ServiceApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceApplication::getApplicantId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ServiceApplication::getStatus, status);
        }
        wrapper.orderByDesc(ServiceApplication::getCreatedAt);
        return applicationMapper.selectList(wrapper);
    }

    @Override
    public IPage<ServiceApplication> getPendingApprovals(Long userId, int page, int pageSize) {
        // 只返回派给当前用户负责的待审卷宗：教师/职工各自只看自己分工内的申请
        LambdaQueryWrapper<ApprovalRecord> recWrapper = new LambdaQueryWrapper<>();
        recWrapper.eq(ApprovalRecord::getAssigneeId, userId)
                  .eq(ApprovalRecord::getAction, "PENDING");
        List<Long> appIds = approvalRecordMapper.selectList(recWrapper).stream()
                .map(ApprovalRecord::getApplicationId).distinct().toList();
        if (appIds.isEmpty()) return new Page<>(page, pageSize);
        LambdaQueryWrapper<ServiceApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ServiceApplication::getId, appIds)
               .orderByDesc(ServiceApplication::getCreatedAt);
        return applicationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public IPage<ServiceApplication> getProcessedApprovals(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<ApprovalRecord> recWrapper = new LambdaQueryWrapper<>();
        recWrapper.eq(ApprovalRecord::getAssigneeId, userId)
                  .in(ApprovalRecord::getAction, "APPROVED", "REJECTED");
        List<Long> appIds = approvalRecordMapper.selectList(recWrapper).stream()
                .map(ApprovalRecord::getApplicationId).distinct().toList();
        if (appIds.isEmpty()) return new Page<>(page, pageSize);
        LambdaQueryWrapper<ServiceApplication> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.in(ServiceApplication::getId, appIds).orderByDesc(ServiceApplication::getCreatedAt);
        return applicationMapper.selectPage(new Page<>(page, pageSize), appWrapper);
    }

    @Override
    public long countPendingApprovals(Long userId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getAssigneeId, userId).eq(ApprovalRecord::getAction, "PENDING");
        return approvalRecordMapper.selectCount(wrapper);
    }

    @Override
    public List<ApprovalRecord> getApprovalTrail(Long applicationId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, applicationId)
               .orderByDesc(ApprovalRecord::getCreatedAt)
               .orderByDesc(ApprovalRecord::getId);
        return approvalRecordMapper.selectList(wrapper);
    }

    /** 当前审批人把申请退回上一步：当前步骤置为「已退回」，上一步重新激活为待审批 */
    @Override
    @Transactional
    public void rollbackApproval(Long applicationId, Long approverId, ApprovalAction dto) {
        ServiceApplication app = applicationMapper.selectById(applicationId);
        if (app == null) throw new BusinessException("申请不存在");
        if (!"PENDING".equals(app.getStatus())) throw new BusinessException("该申请当前状态不允许退回");

        ApprovalRecord current = approvalRecordMapper.selectOne(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getApplicationId, applicationId)
                        .eq(ApprovalRecord::getAction, "PENDING")
                        .orderByAsc(ApprovalRecord::getId)
                        .last("LIMIT 1"));
        if (current == null) throw new BusinessException("无待审批记录");
        if (!current.getAssigneeId().equals(approverId)) throw new BusinessException("您不是该步骤的审批人");

        ApprovalRecord previous = approvalRecordMapper.selectOne(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getApplicationId, applicationId)
                        .eq(ApprovalRecord::getAction, "APPROVED")
                        .lt(ApprovalRecord::getId, current.getId())
                        .orderByDesc(ApprovalRecord::getId)
                        .last("LIMIT 1"));
        if (previous == null) throw new BusinessException("已是首审节点，无法退回上一步");

        current.setAction("ROLLED_BACK");
        current.setComment(dto.getComment());
        approvalRecordMapper.updateById(current);

        previous.setAction("PENDING");
        approvalRecordMapper.updateById(previous);

        app.setCurrentNode(previous.getNodeName());
        applicationMapper.updateById(app);
    }

    /** 申请人催办：仅在申请为处理中可催办，记录催办次数与时间 */
    @Override
    public void urgeApplication(Long applicationId, Long userId) {
        ServiceApplication app = applicationMapper.selectById(applicationId);
        if (app == null) throw new BusinessException("申请不存在");
        if (!"PENDING".equals(app.getStatus())) throw new BusinessException("仅处理中的申请可催办");
        if (!app.getApplicantId().equals(userId)) throw new BusinessException("仅申请人本人可催办");

        int count = app.getUrgeCount() == null ? 0 : app.getUrgeCount();
        app.setUrgeCount(count + 1);
        app.setLastUrgedAt(LocalDateTime.now());
        applicationMapper.updateById(app);
    }

    @Override
    @Transactional
    public void processApproval(Long applicationId, Long approverId, ApprovalAction action) {
        ServiceApplication app = applicationMapper.selectById(applicationId);
        if (app == null) throw new BusinessException("申请不存在");
        if (!"PENDING".equals(app.getStatus())) throw new BusinessException("该申请当前状态不允许审批");

        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, applicationId)
               .eq(ApprovalRecord::getAction, "PENDING")
               .orderByAsc(ApprovalRecord::getId)
               .last("LIMIT 1");
        ApprovalRecord record = approvalRecordMapper.selectOne(wrapper);
        if (record == null) throw new BusinessException("无待审批记录");
        if (!record.getAssigneeId().equals(approverId)) throw new BusinessException("您不是该步骤的审批人");

        record.setAction("APPROVE".equals(action.getAction()) ? "APPROVED" : "REJECTED");
        record.setComment(action.getComment());
        approvalRecordMapper.updateById(record);

        if ("REJECT".equals(action.getAction())) {
            app.setStatus("REJECTED");
            applicationMapper.updateById(app);
            markRemainingAsSkipped(applicationId, record.getId());
            return;
        }

        // 激活下一等待节点；若无后续节点则为末步，办结申请
        LambdaQueryWrapper<ApprovalRecord> waitWrapper = new LambdaQueryWrapper<>();
        waitWrapper.eq(ApprovalRecord::getApplicationId, applicationId)
                   .eq(ApprovalRecord::getAction, "WAIT")
                   .orderByAsc(ApprovalRecord::getId)
                   .last("LIMIT 1");
        ApprovalRecord nextWait = approvalRecordMapper.selectOne(waitWrapper);
        if (nextWait != null) {
            nextWait.setAction("PENDING");
            approvalRecordMapper.updateById(nextWait);
            app.setCurrentNode(nextWait.getNodeName());
            applicationMapper.updateById(app);
        } else {
            app.setStatus("APPROVED");
            app.setCompletedAt(LocalDateTime.now());
            applicationMapper.updateById(app);
        }
    }

    @Override
    @Transactional
    public void initApprovalSteps(Long applicationId) {
        ServiceApplication app = applicationMapper.selectById(applicationId);
        // 第一步：按申请类型路由到对应「群体」负责首审（目录里 reviewRole 决定）
        String firstRole = ServiceCatalogRegistry.reviewRoleOf(app != null ? app.getServiceId() : null);
        User firstUser = pickAssignee(firstRole);
        // 第二步：终审由管理员/超管负责
        User finalUser = pickAssignee("SUPER_ADMIN");
        if (finalUser == null) finalUser = pickAssignee("ADMIN");

        ApprovalRecord step1 = new ApprovalRecord();
        step1.setApplicationId(applicationId);
        String step1Node = "TEACHER".equals(firstRole) ? "教师审批" : "职工审批";
        step1.setNodeName(step1Node);
        step1.setAssigneeId(firstUser != null ? firstUser.getId() : 1L);
        step1.setAssigneeName(firstUser != null ? label(firstUser) : "admin");
        step1.setAction("PENDING");
        approvalRecordMapper.insert(step1);

        // 把当前负责节点写回申请，前端详情可展示「负责群体」
        if (app != null) {
            app.setCurrentNode(step1Node);
            applicationMapper.updateById(app);
        }

        ApprovalRecord step2 = new ApprovalRecord();
        step2.setApplicationId(applicationId);
        step2.setNodeName("管理员审批");
        step2.setAssigneeId(finalUser != null ? finalUser.getId() : 1L);
        step2.setAssigneeName(finalUser != null ? label(finalUser) : "admin");
        step2.setAction("WAIT"); // 第一步未办结前不可见、不可处理
        approvalRecordMapper.insert(step2);
    }

    /** 取某角色的第一位在职用户作为负责审批人，找不到则回退 */
    private User pickAssignee(String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role).eq(User::getStatus, 1).orderByAsc(User::getId).last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private String label(User u) {
        return (u.getRealName() != null && !u.getRealName().isBlank()) ? u.getRealName() : u.getUsername();
    }

    private void markRemainingAsSkipped(Long applicationId, Long currentRecordId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getApplicationId, applicationId)
               .gt(ApprovalRecord::getId, currentRecordId)
               .in(ApprovalRecord::getAction, "PENDING", "WAIT");
        ApprovalRecord skip = new ApprovalRecord();
        skip.setAction("SKIPPED");
        approvalRecordMapper.update(skip, wrapper);
    }
}
