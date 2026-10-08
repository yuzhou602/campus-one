# CampusOne API 与数据模型

本文记录 v1.x 的 HTTP 接口、响应契约和核心数据关系。所有路径默认以 `/api/v1` 开头。

## 1. 通用约定

### 认证

除登录、注册、刷新令牌和退出接口外，API 默认要求：

```http
Authorization: Bearer <access-token>
```

角色限制由后端 `@RequiresRole` 和服务层数据范围共同执行。

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "success": true,
  "timestamp": 1790560800000
}
```

### 分页响应

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "pageSize": 20
}
```

分页参数统一为 `page` 和 `pageSize`。调用方不得依赖 MyBatis-Plus 的 `current`、`size` 字段。

### 错误响应

| HTTP / code | 含义 | 客户端处理 |
| --- | --- | --- |
| `400` | 参数或业务前置条件错误 | 显示服务端消息并保留表单 |
| `401` | 未登录、令牌无效或过期 | 尝试刷新；失败后回到登录页 |
| `403` | 角色、范围或资源归属不允许 | 不重试，提示无权限 |
| `409` | 并发状态已变化或重复数据 | 刷新最新数据后重新判断 |
| `500` | 未预期服务端错误 | 显示通用错误并记录请求上下文 |

## 2. 接口目录

### 认证与用户

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/auth/login` | 用户名密码登录 |
| `POST` | `/auth/register` | 注册学生账号，公开注册不能指定高权限角色 |
| `GET` | `/auth/userinfo` | 获取当前用户展示信息与权限 |
| `GET` | `/auth/me` | 获取当前用户信息，兼容现有客户端 |
| `POST` | `/auth/logout` | 撤销 Access / Refresh Token |
| `POST` | `/auth/refresh` | 使用一次性 Refresh Token 轮换令牌 |
| `GET` | `/users` | 超级管理员分页查询用户 |
| `PUT` | `/users/{id}/data-scope` | 超级管理员配置用户数据范围 |

### 服务目录与事务审批

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/services` | 服务事项、受众、时效、审批路线 |
| `POST` | `/applications` | 提交事务申请 |
| `GET` | `/applications/my` | 分页查询本人申请 |
| `GET` | `/applications/{id}` | 查询有权访问的申请详情 |
| `GET` | `/applications/{id}/approvals` | 查询审批留痕 |
| `POST` | `/applications/{id}/urge` | 申请人催办 |
| `POST` | `/applications/{id}/rollback` | 当前审批人退回上一步 |
| `GET` | `/applications/approvals/pending` | 查询分配给当前用户的待审批申请 |
| `GET` | `/applications/approvals/processed` | 查询当前用户处理过的申请 |
| `GET` | `/applications/approvals/pending/count` | 待审批数量 |
| `POST` | `/applications/approvals/{applicationId}/approve` | 通过当前节点 |
| `POST` | `/applications/approvals/{applicationId}/reject` | 驳回申请 |

### 资源与预约

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/resources` | 查询可用场地列表 |
| `GET` | `/resources/{id}` | 查询可预约场地详情 |
| `GET` | `/resources/{id}/availability?date=` | 查询场地可用时段 |
| `GET` | `/reservations/resources` | 分页查询场地，供 Web 使用 |
| `GET` | `/reservations/availability` | 查询可用时段，兼容 Web 使用 |
| `POST` | `/reservations` | 创建预约 |
| `GET` | `/reservations/my` | 查询本人预约 |
| `GET` | `/reservations/{id}` | 查询本人或管理员可见的预约 |
| `PUT` | `/reservations/{id}/cancel` | 预约人取消预约 |
| `GET` | `/reservations/pending` | 管理员按数据范围查询待审核预约 |
| `POST` | `/reservations/{id}/approve` | 审核通过 |
| `POST` | `/reservations/{id}/reject` | 审核驳回 |

### 报修

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/repairs` | 提交报修 |
| `GET` | `/repairs/my` | 查询本人报修 |
| `GET` | `/repairs/{id}` | 查询报修人、处理人或管理员可见的详情 |
| `GET` | `/repairs/assigned` | 查询分配给当前处理人的工单 |
| `GET` | `/repairs/unassigned` | 管理员查询待派工单 |
| `GET` | `/repairs/technicians` | 查询启用的维修人员安全字段 |
| `POST` | `/repairs/{id}/assign` | 管理员派单 |
| `POST` | `/repairs/{id}/accept` | 被指派维修人员接单 |
| `PUT` | `/repairs/{id}/status` | 按状态机推进工单 |

### 活动

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/activities` | 分页查询活动 |
| `GET` | `/activities/{id}` | 活动详情 |
| `POST` | `/activities/{id}/register` | 报名 |
| `DELETE` | `/activities/{id}/register` | 取消报名 |
| `POST` | `/activities/{id}/checkin` | 签到 |
| `GET` | `/activities/my` | 查询本人参加的活动 |
| `GET` | `/activities/registrations/my` | 查询本人报名记录 |

### 通知与消息

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/notices` | 分页查询本人可见的校园资讯 |
| `POST` | `/notices` | 辅导员或管理员发布通知 |
| `GET` | `/notices/{id}` | 查询可见通知详情 |
| `GET` | `/notices/unread-count` | 未读数量 |
| `PUT` | `/notices/{id}/read` | 标记已读 |
| `PUT` | `/notices/read-all` | 全部标记已读 |
| `DELETE` | `/notices/{id}` | 发布角色删除通知及投递关系 |

`/notifications/**` 是旧 Web/iOS 客户端的兼容入口，内部委托给同一个通知服务。新代码统一使用 `/notices/**`，兼容入口计划在确认旧客户端完成迁移后删除。

### 工作台、分析、文件和 AI

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/dashboard/student` | 学生工作台摘要 |
| `GET` | `/dashboard/teacher` | 教师工作台摘要 |
| `GET` | `/dashboard/admin` | 管理员运营摘要 |
| `GET` | `/tasks/my` | 聚合当前用户审批、预约和报修任务 |
| `GET` | `/analytics/overview` | 管理员总体指标 |
| `GET` | `/analytics/venue` | 场地预约指标 |
| `GET` | `/analytics/repair` | 报修指标 |
| `POST` | `/files/upload` | 上传 JPG、PNG、WebP 或 PDF，最大 10MB |
| `GET` | `/files/{filename}` | 获取已授权文件 |
| `POST` | `/ai/chat` | SSE 规则回复原型 |
| `GET` | `/ai/quick-actions` | 助手快捷问题 |
| `GET` | `/ai/knowledge` | 知识库列表占位接口，当前返回空列表 |
| `GET` | `/system/info` | 系统名称、版本和运行环境 |

## 3. 核心输入模型

### 创建申请

```json
{
  "serviceId": 1,
  "title": "课程请假",
  "content": "",
  "formData": "{\"leaveType\":\"personal\",\"reason\":\"参加竞赛\"}"
}
```

`serviceId` 必须来自 `/services`。未知或停用事项会被拒绝，不能默认路由到任意审批人。

### 创建预约

```json
{
  "resourceId": 1,
  "reservationDate": "2026-10-12",
  "startTime": "14:00",
  "endTime": "16:00",
  "purpose": "课程小组讨论",
  "attendeeCount": 8
}
```

### 提交报修

```json
{
  "location": "信息楼 I205",
  "category": "classroom",
  "description": "投影仪开机后无信号",
  "contact": "13800000000",
  "availableTime": "工作日白天"
}
```

示例号码和账号均为虚构数据，不应替换为真实个人信息后提交到公开仓库。

## 4. 数据模型

### 身份与组织

```text
campus_college 1 ── N campus_major 1 ── N campus_class
        │                    │                  │
        └────────────────────┴──────────────────┘
                             │
                          sys_user
                             │
                 sys_user_role / sys_role_permission
```

`sys_user.role` 为当前主角色，关系表提供权限扩展。`data_scope` 与学院、班级 ID 共同限制管理数据范围。

### 事务审批

```text
campus_service 1 ── N service_application 1 ── N application_approval_record
                              │
                              └── applicant_id ──► sys_user
```

`application_no` 唯一；审批记录保留每次通过、驳回和退回，不覆盖历史意见。

### 预约

```text
campus_building 1 ── N campus_resource 1 ── N resource_reservation
                                                │
                                                ├── user_id ─────► sys_user
                                                └── approved_by ─► sys_user
```

预约使用 `version` 做乐观锁。资源、日期、起止时间和有效状态共同参与冲突判断。

### 报修

```text
sys_user 1 ── N repair_order 1 ── N repair_image
                    │
                    └────────── 1 ── N repair_evaluation
```

工单同时关联报修人和被指派维修人员。状态时间字段记录接单、解决和关闭时间。

### 活动

```text
campus_activity 1 ── N activity_registration N ── 1 sys_user
```

`activity_id + user_id` 唯一，避免重复报名。

### 通知

```text
sys_notification 1 ── N user_notification N ── 1 sys_user
```

用户阅读状态不能写回公共通知行，必须写入 `user_notification`。

## 5. 数据库迁移规则

- `schema.sql` 是 Docker 新库基线，不代替 Flyway 版本管理。
- Flyway 基线版本为 1，后续变化只新增 `V{n}__description.sql`。
- 已发布迁移文件不得修改；修复使用更高版本迁移。
- MySQL DDL 会隐式提交，包含多步 DDL 的迁移需要检查 `information_schema`，保证中断后可重试。
- 大表增加索引或外键前先在等量副本演练，并监控元数据锁。

## 相关文档

- [系统设计](SYSTEM_DESIGN.md)
- [业务流程与状态机](BUSINESS_WORKFLOWS.md)
- [开发与部署](DEVELOPMENT_AND_DEPLOYMENT.md)
