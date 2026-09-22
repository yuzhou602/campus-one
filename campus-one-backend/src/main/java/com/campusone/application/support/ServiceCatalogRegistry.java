package com.campusone.application.support;

import java.util.List;

/**
 * 服务目录注册表：服务项与「负责首审群体」的单一事实来源。
 * 前端 /api/v1/services 与审批路由共用此配置，便于统一维护"申请类型 → 负责群体"。
 */
public final class ServiceCatalogRegistry {

    /** 服务项定义，reviewRole 为首审负责群体（TEACHER=教师 / COUNSELOR=职工） */
    public record ServiceDef(long id, String name, String description, String icon, String reviewRole) {}

    public static final List<ServiceDef> SERVICES = List.of(
        new ServiceDef(1, "请假申请",    "学生请假审批",           "calendar", "TEACHER"),
        new ServiceDef(2, "场地预约",    "教室、实验室等场地预约", "location", "COUNSELOR"),
        new ServiceDef(3, "校园报修",    "宿舍、教室设施报修",     "tools",    "COUNSELOR"),
        new ServiceDef(4, "证明开具",    "在读证明、成绩单等",     "document", "TEACHER"),
        new ServiceDef(5, "活动申请",    "社团活动、讲座申请",     "flag",     "COUNSELOR")
    );

    private ServiceCatalogRegistry() {}

    /** 返回该申请类型的首审负责群体；未知类型默认归「职工」 */
    public static String reviewRoleOf(Long serviceId) {
        if (serviceId == null) return "COUNSELOR";
        return SERVICES.stream()
                .filter(s -> s.id() == serviceId)
                .map(ServiceDef::reviewRole)
                .findFirst()
                .orElse("COUNSELOR");
    }
}