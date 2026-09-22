package com.campusone.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.reservation.entity.CampusResource;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.CampusResourceMapper;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.reservation.service.ReservationService;
import com.campusone.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ResourceReservationMapper, ResourceReservation> implements ReservationService {
    private final CampusResourceMapper resourceMapper;
    private final RedissonClient redissonClient;

    @Override
    @Cacheable(value = "resources", key = "#type + ':' + #buildingId")
    public List<CampusResource> listResources(String type, Long buildingId) {
        LambdaQueryWrapper<CampusResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusResource::getStatus, "AVAILABLE");
        if (type != null) wrapper.eq(CampusResource::getResourceType, type);
        if (buildingId != null) wrapper.eq(CampusResource::getBuildingId, buildingId);
        return resourceMapper.selectList(wrapper);
    }

    @Override
    public IPage<CampusResource> listResourcesPage(Long buildingId, String resourceType, int page, int pageSize) {
        LambdaQueryWrapper<CampusResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CampusResource::getStatus, "AVAILABLE");
        if (buildingId != null) {
            wrapper.eq(CampusResource::getBuildingId, buildingId);
        }
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(CampusResource::getResourceType, resourceType);
        }
        wrapper.orderByAsc(CampusResource::getResourceName);
        return resourceMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public CampusResource getResourceById(Long id) {
        return resourceMapper.selectById(id);
    }

    @Override
    public List<Map<String, Object>> getAvailability(Long resourceId, String date) {
        LocalDate reservationDate = LocalDate.parse(date);

        LambdaQueryWrapper<ResourceReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceReservation::getResourceId, resourceId)
               .eq(ResourceReservation::getReservationDate, reservationDate)
               .in(ResourceReservation::getStatus, "PENDING", "CONFIRMED", "IN_USE");
        List<ResourceReservation> reservations = this.list(wrapper);

        Set<String> occupiedSlots = new HashSet<>();
        for (ResourceReservation r : reservations) {
            occupiedSlots.add(r.getStartTime() + "-" + r.getEndTime());
        }

        String[] times = {"08:00-10:00", "10:00-12:00", "12:00-14:00", "14:00-16:00", "16:00-18:00", "18:00-20:00", "20:00-22:00"};
        List<Map<String, Object>> slots = new ArrayList<>();
        for (String time : times) {
            String[] parts = time.split("-");
            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", parts[0]);
            slot.put("endTime", parts[1]);
            slot.put("available", !occupiedSlots.contains(time));
            slots.add(slot);
        }
        return slots;
    }

    @Override
    @Transactional
    public ResourceReservation createReservation(ResourceReservation reservation, Long userId) {
        String lockKey = "campus:reservation:" + reservation.getResourceId() + ":" +
                reservation.getReservationDate();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            validateTimeRange(reservation.getStartTime(), reservation.getEndTime());
            if (reservation.getReservationDate() == null || reservation.getReservationDate().isBefore(LocalDate.now())) {
                throw new BusinessException("不能预约过去的日期");
            }
            CampusResource resource = resourceMapper.selectById(reservation.getResourceId());
            if (resource == null || !"AVAILABLE".equals(resource.getStatus())) {
                throw new BusinessException("场地不存在或不可预约");
            }
            if (reservation.getParticipantCount() != null
                    && (reservation.getParticipantCount() < 1
                    || reservation.getParticipantCount() > resource.getCapacity())) {
                throw new BusinessException("参与人数超出场地容量");
            }
            LambdaQueryWrapper<ResourceReservation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ResourceReservation::getResourceId, reservation.getResourceId())
                    .eq(ResourceReservation::getReservationDate, reservation.getReservationDate())
                    .lt(ResourceReservation::getStartTime, reservation.getEndTime())
                    .gt(ResourceReservation::getEndTime, reservation.getStartTime())
                    .in(ResourceReservation::getStatus, "PENDING", "CONFIRMED", "IN_USE");
            boolean exists = this.baseMapper.selectCount(wrapper) > 0;
            if (exists) {
                throw new BusinessException("该时段已被预约");
            }
            String no = "RS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                    + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            reservation.setReservationNo(no);
            reservation.setUserId(userId);
            reservation.setStatus("CONFIRMED");
            this.save(reservation);
            return reservation;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统中断");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override
    public IPage<ResourceReservation> getMyReservations(Long userId, int page, int size) {
        return this.page(new Page<>(page, size),
                new LambdaQueryWrapper<ResourceReservation>()
                        .eq(ResourceReservation::getUserId, userId)
                        .orderByDesc(ResourceReservation::getCreatedAt));
    }

    @Override
    @Transactional
    public void cancelReservation(Long id, Long userId) {
        ResourceReservation reservation = this.getById(id);
        if (reservation == null) {
            throw new BusinessException("预约不存在");
        }
        // Authorization: owner or admin can cancel
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role) && !reservation.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权取消此预约");
        }
        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            throw new BusinessException("当前状态不允许取消");
        }
        reservation.setStatus("CANCELLED");
        this.updateById(reservation);
    }

    private void validateTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("预约时间不能为空");
        }
        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startTime);
            end = LocalTime.parse(endTime);
        } catch (Exception e) {
            throw new BusinessException("预约时间格式不正确");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("预约结束时间必须晚于开始时间");
        }
    }
}
