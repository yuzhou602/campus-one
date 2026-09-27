package com.campusone.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.repair.service.RepairService;
import com.campusone.repair.vo.RepairTechnicianVO;
import com.campusone.security.UserContext;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepairServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairService {

    private final UserMapper userMapper;

    private static final Map<String, String> STATUS_FLOW = Map.of(
        "SUBMITTED", "ASSIGNED",
        "ASSIGNED", "ACCEPTED",
        "ACCEPTED", "PROCESSING",
        "PROCESSING", "RESOLVED",
        "RESOLVED", "CONFIRMED",
        "CONFIRMED", "CLOSED"
    );

    @Override
    @Transactional
    public RepairOrder createRepair(RepairOrder order) {
        if (order.getRepairNo() == null) {
            order.setRepairNo("RP" + System.currentTimeMillis());
        }
        if (order.getStatus() == null) {
            order.setStatus("SUBMITTED");
        }
        if (order.getPriority() == null) {
            order.setPriority("MEDIUM");
        }
        this.baseMapper.insert(order);
        return order;
    }

    @Override
    public IPage<RepairOrder> getMyRepairs(Long userId, int page, int size) {
        return this.page(new Page<>(page, size),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getUserId, userId)
                        .orderByDesc(RepairOrder::getCreatedAt));
    }

    @Override
    public IPage<RepairOrder> getAssignedRepairs(Long userId, int page, int size) {
        return this.page(new Page<>(page, size),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getAssignedUserId, userId)
                        .orderByDesc(RepairOrder::getCreatedAt));
    }

    @Override
    public IPage<RepairOrder> getUnassignedRepairs(int page, int size) {
        return this.page(new Page<>(page, size),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getStatus, "SUBMITTED")
                        .isNull(RepairOrder::getAssignedUserId)
                        .orderByAsc(RepairOrder::getCreatedAt));
    }

    @Override
    public List<RepairTechnicianVO> listActiveTechnicians() {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "SERVICE")
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getRealName))
                .stream()
                .map(user -> new RepairTechnicianVO(user.getId(), user.getUsername(), user.getRealName()))
                .toList();
    }

    @Override
    public RepairOrder getRepairById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional
    public void acceptRepair(Long id, Long userId) {
        RepairOrder order = this.getById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        if (!"ASSIGNED".equals(order.getStatus())) {
            throw new BusinessException("当前状态无法接单");
        }
        String operatorRole = UserContext.getCurrentUserRole();
        boolean admin = "ADMIN".equals(operatorRole) || "SUPER_ADMIN".equals(operatorRole);
        if (!admin && (order.getAssignedUserId() == null || !order.getAssignedUserId().equals(userId))) {
            throw new BusinessException(403, "仅被指派的维修人员可以接单");
        }
        LocalDateTime acceptedAt = LocalDateTime.now();
        UpdateWrapper<RepairOrder> update = new UpdateWrapper<RepairOrder>()
                .eq("id", id)
                .eq("status", "ASSIGNED")
                .set("status", "ACCEPTED")
                .set("accepted_at", acceptedAt);
        if (!admin) {
            update.eq("assigned_user_id", userId);
        }
        ensureUpdated(this.baseMapper.update(null, update));
        order.setStatus("ACCEPTED");
        order.setAcceptedAt(acceptedAt);
    }

    @Override
    @Transactional
    public RepairOrder updateStatus(Long id, String status, Long operatorId) {
        RepairOrder order = this.getById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        String nextStatus = STATUS_FLOW.get(order.getStatus());
        if (nextStatus == null || !nextStatus.equals(status)) {
            throw new BusinessException("状态转换不合法");
        }
        String operatorRole = UserContext.getCurrentUserRole();
        authorizeTransition(order, status, operatorId, operatorRole);
        LocalDateTime changedAt = LocalDateTime.now();
        UpdateWrapper<RepairOrder> update = new UpdateWrapper<RepairOrder>()
                .eq("id", id)
                .eq("status", order.getStatus())
                .set("status", status);
        setStatusTimestamp(update, status, changedAt);
        ensureUpdated(this.baseMapper.update(null, update));
        order.setStatus(status);
        updateStatusTimestamp(order, status, changedAt);
        return order;
    }

    @Override
    @Transactional
    public void assignRepair(Long id, Long assigneeId) {
        RepairOrder order = this.getById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"SUBMITTED".equals(order.getStatus())) throw new BusinessException("当前状态无法分配");
        User assignee = userMapper.selectById(assigneeId);
        if (assignee == null || !Integer.valueOf(1).equals(assignee.getStatus())
                || !"SERVICE".equals(assignee.getRole())) {
            throw new BusinessException("只能分配给启用的维修服务人员");
        }
        UpdateWrapper<RepairOrder> update = new UpdateWrapper<RepairOrder>()
                .eq("id", id)
                .eq("status", "SUBMITTED")
                .isNull("assigned_user_id")
                .set("assigned_user_id", assigneeId)
                .set("status", "ASSIGNED");
        ensureUpdated(this.baseMapper.update(null, update));
        order.setAssignedUserId(assigneeId);
        order.setStatus("ASSIGNED");
    }

    private void setStatusTimestamp(UpdateWrapper<RepairOrder> update,
                                    String status, LocalDateTime changedAt) {
        switch (status) {
            case "ACCEPTED" -> update.set("accepted_at", changedAt);
            case "RESOLVED" -> update.set("resolved_at", changedAt);
            case "CLOSED" -> update.set("closed_at", changedAt);
        }
    }

    private void updateStatusTimestamp(RepairOrder order, String status, LocalDateTime changedAt) {
        switch (status) {
            case "ACCEPTED" -> order.setAcceptedAt(changedAt);
            case "RESOLVED" -> order.setResolvedAt(changedAt);
            case "CLOSED" -> order.setClosedAt(changedAt);
        }
    }

    private void ensureUpdated(int updatedRows) {
        if (updatedRows != 1) {
            throw new BusinessException(409, "工单状态已变化，请刷新后重试");
        }
    }

    private void authorizeTransition(RepairOrder order, String targetStatus,
                                     Long operatorId, String operatorRole) {
        if ("ASSIGNED".equals(targetStatus)) {
            throw new BusinessException("请使用工单分配接口指定维修人员");
        }
        boolean admin = "ADMIN".equals(operatorRole) || "SUPER_ADMIN".equals(operatorRole);
        if (admin) return;

        if ("CONFIRMED".equals(targetStatus)) {
            if (!operatorId.equals(order.getUserId())) {
                throw new BusinessException(403, "仅报修人可以确认维修结果");
            }
            return;
        }

        if ("CLOSED".equals(targetStatus)) {
            throw new BusinessException(403, "仅管理员可以执行该操作");
        }

        if (order.getAssignedUserId() == null || !order.getAssignedUserId().equals(operatorId)) {
            throw new BusinessException(403, "仅被指派的维修人员可以更新工单");
        }
    }
}
