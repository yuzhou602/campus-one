package com.campusone.application.support;

import java.util.List;

/**
 * 服务目录注册表：服务项与「负责首审群体」的单一事实来源。
 * 前端 /api/v1/services 与审批路由共用此配置，便于统一维护"申请类型 → 负责群体"。
 */
public final class ServiceCatalogRegistry {

    /** 服务项定义。reviewRole 为首审负责群体（TEACHER=教师 / COUNSELOR=职工）。 */
    public record ServiceDef(long id, String name, String description, String icon,
                             String audience, String duration, String approvalFlow,
                             String reviewRole) {}

    public static final List<ServiceDef> SERVICES = List.of(
        new ServiceDef(1, "请假申请", "课程、实习与日常请假在线登记", "Calendar",
                "全体学生", "2 个工作日", "教师 → 管理员", "TEACHER"),
        new ServiceDef(2, "学生证明申请", "在读证明、成绩证明等材料申请", "Document",
                "全体学生", "1 个工作日", "职工 → 管理员", "COUNSELOR"),
        new ServiceDef(3, "场地特殊使用", "常规预约时段以外的场地使用申请", "Location",
                "师生", "3 个工作日", "职工 → 管理员", "COUNSELOR"),
        new ServiceDef(4, "活动场地申请", "社团和班级活动场地备案", "Flag",
                "学生组织", "3 个工作日", "职工 → 管理员", "COUNSELOR"),
        new ServiceDef(5, "物品借用申请", "公共器材和活动物资借用", "Box",
                "师生", "1 个工作日", "职工 → 管理员", "COUNSELOR"),
        new ServiceDef(6, "宿舍事务申请", "调宿、晚归等宿舍事务登记", "House",
                "住宿学生", "2 个工作日", "职工 → 管理员", "COUNSELOR")
    );

    private ServiceCatalogRegistry() {}

    public static ServiceDef require(Long serviceId) {
        if (serviceId == null) {
            throw new IllegalArgumentException("服务事项不能为空");
        }
        return SERVICES.stream()
                .filter(service -> service.id() == serviceId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("服务事项不存在或已停用"));
    }

    /** 返回该申请类型的首审负责群体。未知类型必须拒绝，不能静默改变审批路线。 */
    public static String reviewRoleOf(Long serviceId) {
        return require(serviceId).reviewRole();
    }
}
