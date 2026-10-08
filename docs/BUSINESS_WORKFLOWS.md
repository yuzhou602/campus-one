# CampusOne 业务流程与状态机

本文描述当前代码允许的状态变化、执行角色和并发规则。状态只能通过对应业务动作改变，不能由客户端任意写入。

## 1. 事务申请与审批

### 主流程

```text
申请人提交
   │
   ▼
PENDING：教师或职工首审
   │ 通过
   ▼
PENDING：管理员终审
   ├───────────────┐
   │通过           │驳回
   ▼               ▼
APPROVED        REJECTED
已归档           已驳回
```

服务事项决定首审群体：

| 服务事项 | 首审 | 终审 |
| --- | --- | --- |
| 请假申请 | `TEACHER` | `ADMIN` 或 `SUPER_ADMIN` |
| 学生证明申请 | `COUNSELOR` | `ADMIN` 或 `SUPER_ADMIN` |
| 场地特殊使用 | `COUNSELOR` | `ADMIN` 或 `SUPER_ADMIN` |
| 活动场地申请 | `COUNSELOR` | `ADMIN` 或 `SUPER_ADMIN` |
| 物品借用申请 | `COUNSELOR` | `ADMIN` 或 `SUPER_ADMIN` |
| 宿舍事务申请 | `COUNSELOR` | `ADMIN` 或 `SUPER_ADMIN` |

审批人必须处于启用状态，并且其 `data_scope` 覆盖申请人的学院、班级或本人范围。存在多个候选人时，系统按申请 ID 稳定分摊。

### 审批记录状态

| 状态 | 含义 |
| --- | --- |
| `WAIT` | 后续节点，尚未激活 |
| `PENDING` | 当前可处理节点 |
| `APPROVED` | 该节点已通过 |
| `REJECTED` | 该节点已驳回 |
| `ROLLED_BACK` | 后续节点执行了退回 |
| `SKIPPED` | 申请被驳回后不再执行的节点 |

同一申请同时只能存在一个可处理节点。通过当前节点后激活最早的 `WAIT` 节点；没有等待节点时申请归档。驳回会终止申请并跳过剩余节点。

### 退回与催办

- 退回只能由当前审批人执行。
- 首审节点没有上一步，不能退回。
- 退回时当前节点记为 `ROLLED_BACK`，上一已通过节点重新变为 `PENDING`，并为原当前节点创建新的 `WAIT` 记录，保证重新通过后仍需完成终审。
- 催办只能由申请人本人在 `PENDING` 状态执行；系统记录次数与最后催办时间。

### 并发规则

审批更新同时匹配记录 ID、审批人和旧动作 `PENDING`。两个请求同时处理时，只有一个能更新一行，另一个返回 `409`。

## 2. 资源预约

```text
创建预约
   ├─ 普通场地 ──────────► CONFIRMED
   └─ 需审核场地 ─► PENDING ─┬─ 通过 ─► CONFIRMED
                              └─ 驳回 ─► REJECTED

PENDING / CONFIRMED ──用户取消──► CANCELLED
```

当前代码还预留 `IN_USE`、`COMPLETED` 和 `EXPIRED` 状态，用于后续签到、离场和定时过期任务；v1.x 没有公开状态推进接口。

创建预约时系统验证：

1. 日期不能早于当天。
2. 结束时间必须晚于开始时间。
3. 场地存在且状态为 `AVAILABLE`。
4. 参与人数不少于 1 且不超过容量。
5. 同一资源、日期和时间区间不能与 `PENDING`、`CONFIRMED`、`IN_USE` 预约重叠。

系统按“资源 ID + 日期”获取 Redisson 锁，在锁内执行冲突检查和写入。审核和取消使用条件更新或乐观锁，避免重复操作。

管理员审核列表和审核动作都必须遵守管理员的数据范围，不能因为拥有 `ADMIN` 角色就自动读取其他学院的数据。

## 3. 报修工单

```text
SUBMITTED
   │ 管理员派单
   ▼
ASSIGNED
   │ 被指派人员接单
   ▼
ACCEPTED
   │ 开始处理
   ▼
PROCESSING
   │ 标记解决
   ▼
RESOLVED
   │ 报修人确认
   ▼
CONFIRMED
   │ 管理员关闭
   ▼
CLOSED
```

角色约束：

- `SUBMITTED → ASSIGNED` 只能通过派单接口完成，且接收人必须是启用的 `SERVICE` 用户。
- 非管理员只能接收分派给自己的工单。
- `ACCEPTED → PROCESSING → RESOLVED` 由被指派维修人员推进。
- `RESOLVED → CONFIRMED` 只能由报修人本人执行。
- `CONFIRMED → CLOSED` 只能由管理员执行。

每次更新都匹配旧状态。状态已被其他请求改变时返回 `409`，客户端应刷新后重新判断可用操作。

`WAITING_PART` 和 `CANCELLED` 已出现在展示状态中，但当前服务层尚未提供对应转换；它们属于后续扩展，不能由前端直接提交。

## 4. 校园活动

活动只有 `ACTIVE` 状态允许报名和签到。

```text
未报名 ──报名──► 已报名 ──签到──► 已签到
   ▲                │
   └────取消报名────┘  （签到后不可取消）
```

报名需要满足：

- 不存在同一用户、同一活动的报名记录；
- 活动未超过报名截止时间；
- `registered_count < capacity`；
- 活动状态为 `ACTIVE`。

系统使用活动级 Redisson 锁、`activity_id + user_id` 唯一键和人数条件更新三层保护。签到从活动开始前 30 分钟开放，到活动结束时关闭。

## 5. 通知与消息

```text
发布通知
   │
   ├─ ALL  ─► 为启用用户创建 user_notification
   └─ USER ─► 为指定用户创建 user_notification
                    │
                    ├─ 未读
                    └─ 已读（记录 read_at）
```

通知正文保存在 `sys_notification`，每个用户的投递和阅读状态保存在 `user_notification`。删除通知前先删除用户投递关系；数据库外键也对遗留数据提供完整性保护。

当前存在 `/notices` 和 `/notifications` 两组兼容接口。新前端分别把“校园资讯”和“消息中心”映射到这两组接口。后续版本应统一为单一通知领域接口，并保留短期兼容层。

## 6. 登录与令牌

```text
用户名密码登录
   │
   ├─ access token：访问 API
   └─ refresh token：轮换 access token

刷新成功：旧 refresh token 加入黑名单，签发一对新 token
退出登录：access token 和 refresh token 都加入黑名单
```

刷新令牌只能使用一次。黑名单表对完整 token 建唯一索引，重复退出保持幂等。

## 7. 前端角色流程

- 学生：智慧首页 → 办事大厅 / 预约 / 报修 / 活动 → 我的记录。
- 教师与辅导员：智慧首页 → 审批中心 → 卷宗详情 → 处理并查看留痕。
- 服务人员：维修工作台 → 接单 → 推进处理 → 等待用户确认。
- 管理员：运营首页 → 预约审核 / 工单派发 / 数据中心。
- 超级管理员：管理员能力 + 用户数据范围配置和系统管理。

## 相关文档

- [系统设计](SYSTEM_DESIGN.md)
- [API 与数据模型](API_AND_DATA_MODEL.md)
- [开发与部署](DEVELOPMENT_AND_DEPLOYMENT.md)
