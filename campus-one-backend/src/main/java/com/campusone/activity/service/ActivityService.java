package com.campusone.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.activity.entity.ActivityRegistration;
import com.campusone.activity.entity.CampusActivity;

public interface ActivityService extends IService<CampusActivity> {
    IPage<CampusActivity> listActivities(int page, int size, String category);
    CampusActivity getActivityById(Long id);
    void registerActivity(Long activityId, Long userId);
    IPage<CampusActivity> getMyActivities(Long userId, int page, int size);
    CampusActivity updateActivity(Long id, CampusActivity activity);
    void deleteActivity(Long id);
    void cancelRegistration(Long activityId, Long userId);
    void checkin(Long activityId, Long userId);
    IPage<ActivityRegistration> getMyRegistrations(Long userId, int page, int pageSize);
}
