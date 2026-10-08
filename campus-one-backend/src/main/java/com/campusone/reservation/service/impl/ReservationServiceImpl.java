package com.campusone.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.common.util.BusinessNumberGenerator;
import com.campusone.reservation.entity.CampusResource;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.CampusResourceMapper;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.reservation.service.ReservationService;
import com.campusone.security.UserContext;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ResourceReservationMapper, ResourceReservation> implements ReservationService {
    private final CampusResourceMapper resourceMapper;
    private final UserMapper userMapper;
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
        if (resourceMapper.selectById(resourceId) == null) {
            throw new BusinessException("场地不存在");
        }
        LocalDate reservationDate;
        try {
            reservationDate = LocalDate.parse(date);
        } catch (Exception e) {
            throw new BusinessException("预约日期格式不正确");
        }
        if (reservationDate.isBefore(LocalDate.now())) {
            throw new BusinessException("不能查询过去日期的可用时段");
        }

        LambdaQueryWrapper<ResourceReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceReservation::getResourceId, resourceId)
               .eq(ResourceReservation::getReservationDate, reservationDate)
               .in(ResourceReservation::getStatus, "PENDING", "CONFIRMED", "IN_USE");
        List<ResourceReservation> reservations = this.list(wrapper);

        String[] times = {"08:00-10:00", "10:00-12:00", "12:00-14:00", "14:00-16:00", "16:00-18:00", "18:00-20:00", "20:00-22:00"};
        List<Map<String, Object>> slots = new ArrayList<>();
        for (String time : times) {
            String[] parts = time.split("-");
            LocalTime slotStart = LocalTime.parse(parts[0]);
            LocalTime slotEnd = LocalTime.parse(parts[1]);
            boolean occupied = reservations.stream().anyMatch(reservation ->
                    overlaps(slotStart, slotEnd,
                            parseStoredTime(reservation.getStartTime()),
                            parseStoredTime(reservation.getEndTime())));
            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", parts[0]);
            slot.put("endTime", parts[1]);
            slot.put("available", !occupied);
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
            reservation.setReservationNo(BusinessNumberGenerator.generate("RS"));
            reservation.setUserId(userId);
            reservation.setStatus(Boolean.TRUE.equals(resource.getNeedApproval()) ? "PENDING" : "CONFIRMED");
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
        IPage<ResourceReservation> result = this.page(new Page<>(page, size),
                new LambdaQueryWrapper<ResourceReservation>()
                        .eq(ResourceReservation::getUserId, userId)
                        .orderByDesc(ResourceReservation::getCreatedAt));
        return enrichReservations(result);
    }

    @Override
    public IPage<ResourceReservation> getPendingReservations(Long reviewerId, int page, int size) {
        User reviewer = loadReservationReviewer(reviewerId);
        IPage<ResourceReservation> result = this.baseMapper.selectPendingForScope(
                new Page<>(page, size), reviewerId, effectiveScope(reviewer),
                reviewer.getCollegeId(), reviewer.getClassId());
        return enrichReservations(result);
    }

    private IPage<ResourceReservation> enrichReservations(IPage<ResourceReservation> page) {
        List<ResourceReservation> records = page.getRecords();
        if (records == null || records.isEmpty()) return page;

        Set<Long> resourceIds = records.stream()
                .map(ResourceReservation::getResourceId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> userIds = records.stream()
                .map(ResourceReservation::getUserId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, String> resourceNames = resourceIds.isEmpty() ? Map.of()
                : resourceMapper.selectBatchIds(resourceIds).stream()
                .collect(java.util.stream.Collectors.toMap(CampusResource::getId, CampusResource::getResourceName));
        Map<Long, String> userNames = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, this::displayName));

        records.forEach(record -> {
            record.setResourceName(resourceNames.get(record.getResourceId()));
            record.setUserName(userNames.get(record.getUserId()));
        });
        return page;
    }

    private String displayName(User user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) return user.getRealName();
        return user.getUsername() == null ? "" : user.getUsername();
    }

    private User loadReservationReviewer(Long reviewerId) {
        User reviewer = userMapper.selectById(reviewerId);
        if (reviewer == null || !Integer.valueOf(1).equals(reviewer.getStatus())
                || !("ADMIN".equals(reviewer.getRole()) || "SUPER_ADMIN".equals(reviewer.getRole()))) {
            throw new BusinessException(403, "无权审核预约");
        }
        return reviewer;
    }

    private String effectiveScope(User reviewer) {
        if ("SUPER_ADMIN".equals(reviewer.getRole())) return "ALL";
        return reviewer.getDataScope() == null ? "SELF" : reviewer.getDataScope();
    }

    private void authorizeReservationReview(Long reviewerId, Long applicantId) {
        User reviewer = loadReservationReviewer(reviewerId);
        if ("ALL".equals(effectiveScope(reviewer))) return;
        User applicant = userMapper.selectById(applicantId);
        if (applicant == null) throw new BusinessException("预约申请人不存在");
        boolean allowed = switch (effectiveScope(reviewer)) {
            case "COLLEGE" -> reviewer.getCollegeId() != null
                    && reviewer.getCollegeId().equals(applicant.getCollegeId());
            case "CLASS" -> reviewer.getClassId() != null
                    && reviewer.getClassId().equals(applicant.getClassId());
            default -> reviewer.getId().equals(applicant.getId());
        };
        if (!allowed) throw new BusinessException(403, "无权审核该范围的预约");
    }

    @Override
    @Transactional
    public void reviewReservation(Long id, boolean approved, String comment, Long reviewerId) {
        ResourceReservation reservation = this.getById(id);
        if (reservation == null) throw new BusinessException("预约不存在");
        if (!"PENDING".equals(reservation.getStatus())) {
            throw new BusinessException("该预约已经处理，不能重复审核");
        }
        if (!approved && (comment == null || comment.isBlank())) {
            throw new BusinessException("驳回预约时必须填写原因");
        }
        authorizeReservationReview(reviewerId, reservation.getUserId());
        reservation.setStatus(approved ? "CONFIRMED" : "REJECTED");
        reservation.setApprovedBy(reviewerId);
        reservation.setApprovalRemark(comment == null ? null : comment.trim());
        reservation.setApprovedAt(LocalDateTime.now());
        if (!this.updateById(reservation)) {
            throw new BusinessException(409, "预约状态已变化，请刷新后重试");
        }
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
        if (!this.updateById(reservation)) {
            throw new BusinessException(409, "预约状态已变化，请刷新后重试");
        }
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

    private LocalTime parseStoredTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            throw new BusinessException("已有预约时间数据格式不正确，请联系管理员");
        }
    }

    private boolean overlaps(LocalTime firstStart, LocalTime firstEnd,
                             LocalTime secondStart, LocalTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }
}
