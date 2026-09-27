package com.campusone.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.vo.RepairTechnicianVO;

import java.util.List;

public interface RepairService extends IService<RepairOrder> {
    RepairOrder createRepair(RepairOrder order);
    IPage<RepairOrder> getMyRepairs(Long userId, int page, int size);
    IPage<RepairOrder> getAssignedRepairs(Long userId, int page, int size);
    IPage<RepairOrder> getUnassignedRepairs(int page, int size);
    List<RepairTechnicianVO> listActiveTechnicians();
    RepairOrder getRepairById(Long id);
    void acceptRepair(Long id, Long userId);
    RepairOrder updateStatus(Long id, String status, Long userId);
    void assignRepair(Long id, Long assigneeId);
}
