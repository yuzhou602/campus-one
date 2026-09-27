package com.campusone.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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

        ensureApprovalUpdated(approvalRecordMapper.update(null, new UpdateWrapper<ApprovalRecord>()
                .eq("id", current.getId())
                .eq("assignee_id", approverId)
                .eq("action", "PENDING")
                .set("action", "ROLLED_BACK")
                .set("comment", dto.getComment())));
        ensureApprovalUpdated(approvalRecordMapper.update(null, new UpdateWrapper<ApprovalRecord>()
                .eq("id", previous.getId())
                .eq("action", "APPROVED")
                .set("action", "PENDING")));
        current.setAction("ROLLED_BACK");
        current.setComment(dto.getComment());
        previous.setAction("PENDING");

        ApprovalRecord retry = new ApprovalRecord();
        retry.setApplicationId(applicationId);
        retry.setTaskId(current.getTaskId());
        retry.setNodeName(current.getNodeName());
        retry.setAssigneeId(current.getAssigneeId());
        retry.setAssigneeName(current.getAssigneeName());
        retry.setAction("WAIT");
        approvalRecordMapper.insert(retry);

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
        if (action == null || !("APPROVE".equals(action.getAction()) || "REJECT".equals(action.getAction()))) {
            throw new BusinessException("审批动作不合法");
        }
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

        String decision = "APPROVE".equals(action.getAction()) ? "APPROVED" : "REJECTED";
        ensureApprovalUpdated(approvalRecordMapper.update(null, new UpdateWrapper<ApprovalRecord>()
                .eq("id", record.getId())
                .eq("assignee_id", approverId)
                .eq("action", "PENDING")
                .set("action", decision)
                .set("comment", action.getComment())));
        record.setAction(decision);
        record.setComment(action.getComment());

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
            ensureApprovalUpdated(approvalRecordMapper.update(null, new UpdateWrapper<ApprovalRecord>()
                    .eq("id", nextWait.getId())
                    .eq("action", "WAIT")
                    .set("action", "PENDING")));
            nextWait.setAction("PENDING");
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
        if (app == null) throw new BusinessException("申请不存在");
        User applicant = userMapper.selectById(app.getApplicantId());
        if (applicant == null || !Integer.valueOf(1).equals(applicant.getStatus())) {
            throw new BusinessException("申请人不存在或账号已停用");
        }
        // 第一步：按申请类型路由到对应「群体」负责首审（目录里 reviewRole 决定）
        String firstRole = ServiceCatalogRegistry.reviewRoleOf(app.getServiceId());
        User firstUser = pickScopedAssignee(firstRole, applicant, applicationId);
        // 第二步：终审由管理员/超管负责
        User finalUser = pickScopedAssignee("ADMIN", applicant, applicationId);
        if (finalUser == null) finalUser = pickScopedAssignee("SUPER_ADMIN", applicant, applicationId);
        if (firstUser == null) {
            throw new BusinessException("未找到可处理该申请的" + roleLabel(firstRole) + "，请联系管理员配置组织和数据范围");
        }
        if (finalUser == null) {
            throw new BusinessException("未找到可处理该申请的管理员，请联系系统管理员");
        }

        ApprovalRecord step1 = new ApprovalRecord();
        step1.setApplicationId(applicationId);
        String step1Node = "TEACHER".equals(firstRole) ? "教师审批" : "职工审批";
        step1.setNodeName(step1Node);
        step1.setAssigneeId(firstUser.getId());
        step1.setAssigneeName(label(firstUser));
        step1.setAction("PENDING");
        approvalRecordMapper.insert(step1);

        // 把当前负责节点写回申请，前端详情可展示「负责群体」
        app.setCurrentNode(step1Node);
        applicationMapper.updateById(app);

        ApprovalRecord step2 = new ApprovalRecord();
        step2.setApplicationId(applicationId);
        step2.setNodeName("管理员审批");
        step2.setAssigneeId(finalUser.getId());
        step2.setAssigneeName(label(finalUser));
        step2.setAction("WAIT"); // 第一步未办结前不可见、不可处理
        approvalRecordMapper.insert(step2);
    }

    /**
     * 只从能够覆盖申请人组织范围的在职用户中选择审批人，并按申请号在候选人之间分摊。
     * 不再回退到固定用户，避免跨学院越权或把全部申请压给第一个账号。
     */
    private User pickScopedAssignee(String role, User applicant, Long applicationId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role)
                .eq(User::getStatus, 1)
                .and(scope -> {
                    scope.eq(User::getDataScope, "ALL");
                    if (applicant.getCollegeId() != null) {
                        scope.or(college -> college.eq(User::getDataScope, "COLLEGE")
                                .eq(User::getCollegeId, applicant.getCollegeId()));
                    }
                    if (applicant.getClassId() != null) {
                        scope.or(clazz -> clazz.eq(User::getDataScope, "CLASS")
                                .eq(User::getClassId, applicant.getClassId()));
                    }
                    scope.or(self -> self.eq(User::getId, applicant.getId())
                            .and(dataScope -> dataScope.eq(User::getDataScope, "SELF")
                                    .or().isNull(User::getDataScope)));
                })
                .orderByAsc(User::getId);
        List<User> candidates = userMapper.selectList(wrapper).stream()
                .filter(candidate -> canManage(candidate, applicant))
                .toList();
        if (candidates.isEmpty()) return null;
        int index = Math.floorMod(applicationId, candidates.size());
        return candidates.get(index);
    }

    private boolean canManage(User candidate, User applicant) {
        String scope = candidate.getDataScope();
        if ("ALL".equals(scope)) return true;
        if ("COLLEGE".equals(scope)) {
            return candidate.getCollegeId() != null && candidate.getCollegeId().equals(applicant.getCollegeId());
        }
        if ("CLASS".equals(scope)) {
            return candidate.getClassId() != null && candidate.getClassId().equals(applicant.getClassId());
        }
        return candidate.getId().equals(applicant.getId());
    }

    private String roleLabel(String role) {
        return "TEACHER".equals(role) ? "教师审批人" : "职工审批人";
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

    private void ensureApprovalUpdated(int updatedRows) {
        if (updatedRows != 1) {
            throw new BusinessException(409, "审批状态已变化，请刷新后重试");
        }
    }
}
