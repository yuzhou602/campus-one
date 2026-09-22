package com.campusone.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.activity.entity.ActivityRegistration;
import com.campusone.activity.entity.CampusActivity;
import com.campusone.activity.mapper.ActivityRegistrationMapper;
import com.campusone.activity.mapper.CampusActivityMapper;
import com.campusone.activity.service.ActivityService;
import com.campusone.common.exception.BusinessException;
import com.campusone.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<CampusActivityMapper, CampusActivity> implements ActivityService {
    private final ActivityRegistrationMapper registrationMapper;
    private final RedissonClient redissonClient;

    @Override
    @Cacheable(value = "activities", key = "#category + ':' + #page + ':' + #size")
    public IPage<CampusActivity> listActivities(int page, int size, String category) {
        LambdaQueryWrapper<CampusActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusActivity::getStatus, "ACTIVE");
        if (category != null && !"all".equals(category)) {
            wrapper.eq(CampusActivity::getCategory, category);
        }
        wrapper.orderByDesc(CampusActivity::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public CampusActivity getActivityById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional
    public void registerActivity(Long activityId, Long userId) {
        String lockKey = "campus:activity:register:" + activityId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            // Check duplicate registration
            boolean exists = registrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .eq(ActivityRegistration::getUserId, userId)
            ) > 0;
            if (exists) throw new BusinessException("已报名该活动");

            CampusActivity activity = this.getById(activityId);
            if (activity == null) throw new BusinessException("活动不存在");
            if (!"ACTIVE".equals(activity.getStatus())) throw new BusinessException("活动当前不可报名");
            int registeredCount = activity.getRegisteredCount() == null ? 0 : activity.getRegisteredCount();
            int capacity = activity.getCapacity() == null ? 0 : activity.getCapacity();
            if (capacity <= 0 || registeredCount >= capacity) {
                throw new BusinessException("活动名额已满");
            }
            if (activity.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
                throw new BusinessException("报名已截止");
            }

            // Register
            ActivityRegistration reg = new ActivityRegistration();
            reg.setActivityId(activityId);
            reg.setUserId(userId);
            reg.setCheckedIn(false);
            registrationMapper.insert(reg);

            // Atomic count increment using SQL (avoids read-then-write race)
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CampusActivity> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            updateWrapper.eq(CampusActivity::getId, activityId)
                .apply("registered_count < capacity")
                .setSql("registered_count = registered_count + 1");
            if (this.baseMapper.update(null, updateWrapper) == 0) {
                throw new BusinessException("活动名额已满");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统中断");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override
    public IPage<CampusActivity> getMyActivities(Long userId, int page, int size) {
        return this.page(new Page<>(page, size),
            new LambdaQueryWrapper<CampusActivity>()
                .in(CampusActivity::getId, new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getUserId, userId)
                    .select(ActivityRegistration::getActivityId))
                .orderByDesc(CampusActivity::getCreatedAt));
    }

    @Override
    public CampusActivity updateActivity(Long id, CampusActivity activity) {
        CampusActivity existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role) && !"COUNSELOR".equals(role)
                && !Objects.equals(existing.getCreatorId(), UserContext.getCurrentUserId())) {
            throw new BusinessException(403, "无权修改此活动");
        }
        activity.setId(id);
        this.updateById(activity);
        return activity;
    }

    @Override
    public void deleteActivity(Long id) {
        CampusActivity existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)
                && !Objects.equals(existing.getCreatorId(), UserContext.getCurrentUserId())) {
            throw new BusinessException(403, "无权删除此活动");
        }
        this.removeById(id);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long activityId, Long userId) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
               .eq(ActivityRegistration::getUserId, userId);
        ActivityRegistration reg = registrationMapper.selectOne(wrapper);
        if (reg == null) {
            throw new BusinessException("未报名此活动");
        }
        if (Boolean.TRUE.equals(reg.getCheckedIn())) {
            throw new BusinessException("已签到，不能取消报名");
        }
        registrationMapper.deleteById(reg.getId());
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CampusActivity> updateWrapper =
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(CampusActivity::getId, activityId)
            .apply("registered_count > 0")
            .setSql("registered_count = registered_count - 1");
        this.baseMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional
    public void checkin(Long activityId, Long userId) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
               .eq(ActivityRegistration::getUserId, userId);
        ActivityRegistration reg = registrationMapper.selectOne(wrapper);
        if (reg == null) {
            throw new BusinessException("未报名此活动");
        }
        if (Boolean.TRUE.equals(reg.getCheckedIn())) {
            throw new BusinessException("请勿重复签到");
        }
        CampusActivity activity = this.getById(activityId);
        if (activity == null) throw new BusinessException("活动不存在");
        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equals(activity.getStatus())) throw new BusinessException("活动当前不可签到");
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime().minusMinutes(30))) {
            throw new BusinessException("签到尚未开始");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动已结束");
        }
        reg.setCheckedIn(true);
        reg.setCheckedInAt(now);
        registrationMapper.updateById(reg);
    }

    @Override
    public IPage<ActivityRegistration> getMyRegistrations(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getUserId, userId);
        wrapper.orderByDesc(ActivityRegistration::getRegisteredAt);
        return registrationMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
