package com.campusone.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.repair.service.RepairService;
import com.campusone.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairService {

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
        if (!"ADMIN".equals(operatorRole) && !"SUPER_ADMIN".equals(operatorRole) && !"SERVICE".equals(operatorRole)) {
            if (order.getAssignedUserId() == null || !order.getAssignedUserId().equals(userId)) {
                throw new BusinessException(403, "无权操作此工单");
            }
        }
        order.setStatus("ACCEPTED");
        order.setAssignedUserId(userId);
        order.setAcceptedAt(LocalDateTime.now());
        this.updateById(order);
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
        // Authorization: only assigned technician, service staff, or admin can update status
        String operatorRole = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(operatorRole) && !"SUPER_ADMIN".equals(operatorRole) && !"SERVICE".equals(operatorRole)) {
            if (order.getAssignedUserId() == null || !order.getAssignedUserId().equals(operatorId)) {
                throw new BusinessException(403, "无权操作此工单");
            }
        }
        order.setStatus(status);
        updateStatusTimestamp(order, status);
        this.updateById(order);
        return order;
    }

    @Override
    @Transactional
    public void assignRepair(Long id, Long assigneeId) {
        RepairOrder order = this.getById(id);
        if (order == null) throw new BusinessException("工单不存在");
        if (!"SUBMITTED".equals(order.getStatus())) throw new BusinessException("当前状态无法分配");
        order.setAssignedUserId(assigneeId);
        order.setStatus("ASSIGNED");
        this.updateById(order);
    }

    private void updateStatusTimestamp(RepairOrder order, String status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case "ACCEPTED" -> order.setAcceptedAt(now);
            case "RESOLVED" -> order.setResolvedAt(now);
        }
    }
}
