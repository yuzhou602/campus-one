package com.campusone.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.application.entity.ServiceApplication;
import com.campusone.application.mapper.ServiceApplicationMapper;
import com.campusone.application.service.ApplicationService;
import com.campusone.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ApplicationServiceImpl extends ServiceImpl<ServiceApplicationMapper, ServiceApplication> implements ApplicationService {

    @Override
    public ServiceApplication createApplication(ServiceApplication app, Long userId) {
        String no = "AP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        app.setApplicationNo(no);
        app.setApplicantId(userId);
        app.setStatus("SUBMITTED");
        app.setSubmittedAt(LocalDateTime.now());
        app.setCurrentNode("班主任审批");
        this.save(app);
        return app;
    }

    @Override
    public IPage<ServiceApplication> getMyApplications(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<ServiceApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceApplication::getApplicantId, userId);
        if (status != null && !"all".equals(status)) {
            wrapper.eq(ServiceApplication::getStatus, status);
        }
        wrapper.orderByDesc(ServiceApplication::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public ServiceApplication getApplicationById(Long id) {
        return this.getById(id);
    }

    @Override
    public IPage<ServiceApplication> getPendingApprovals(int page, int size) {
        return this.page(new Page<>(page, size),
            new LambdaQueryWrapper<ServiceApplication>()
                .eq(ServiceApplication::getStatus, "SUBMITTED")
                .orderByDesc(ServiceApplication::getCreatedAt));
    }
}
