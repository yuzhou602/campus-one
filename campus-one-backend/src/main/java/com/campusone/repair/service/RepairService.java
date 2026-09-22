package com.campusone.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.repair.entity.RepairOrder;

public interface RepairService extends IService<RepairOrder> {
    RepairOrder createRepair(RepairOrder order);
    IPage<RepairOrder> getMyRepairs(Long userId, int page, int size);
    IPage<RepairOrder> getAssignedRepairs(Long userId, int page, int size);
    RepairOrder getRepairById(Long id);
    void acceptRepair(Long id, Long userId);
    RepairOrder updateStatus(Long id, String status, Long userId);
    void assignRepair(Long id, Long assigneeId);
}
