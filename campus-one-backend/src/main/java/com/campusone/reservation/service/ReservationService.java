package com.campusone.reservation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.reservation.entity.CampusResource;
import com.campusone.reservation.entity.ResourceReservation;

import java.util.List;
import java.util.Map;

public interface ReservationService extends IService<ResourceReservation> {
    List<CampusResource> listResources(String type, Long buildingId);
    IPage<CampusResource> listResourcesPage(Long buildingId, String resourceType, int page, int pageSize);
    CampusResource getResourceById(Long id);
    List<Map<String, Object>> getAvailability(Long resourceId, String date);
    ResourceReservation createReservation(ResourceReservation reservation, Long userId);
    IPage<ResourceReservation> getMyReservations(Long userId, int page, int size);
    void cancelReservation(Long id, Long userId);
}
