package com.campusone.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.application.entity.ServiceApplication;

public interface ApplicationService extends IService<ServiceApplication> {
    ServiceApplication createApplication(ServiceApplication app, Long userId);
    IPage<ServiceApplication> getMyApplications(Long userId, int page, int size, String status);
    ServiceApplication getApplicationById(Long id);
    IPage<ServiceApplication> getPendingApprovals(int page, int size);
}
