# 智慧校园综合服务平台 · iOS 端同步清单

> 与 Vue 端（`campus-one-frontend`）保持功能、交互与设计语言同步。
> 设计语言统一采用「校园档案 · 纸张与墨水」（纯黑白、卡纸面、墨线、印章），与 Web 端 `index.css` 令牌一致。
> iOS 端为原生 **UIKit（非 SwiftUI）**，当前环境（Windows/无 Xcode）不可本地编译，交付面向 Xcode 工程的源码与装配配置（`project.yml`，用 XcodeGen 生成 `.xcodeproj`）。

## 设计令牌（= Web 端 @theme）

| 令牌 | 值 | 用途 |
|---|---|---|
| paper    | `#F3EFE7` | 页面底：旧纸米黄 |
| surface  | `#FCFAF6` | 档案卡纸面 |
| ink900   | `#221D18` | 主文字 / 标题（浓墨） |
| ink700   | `#4A443C` | 悬停 / 强次级 |
| ink500   | `#6E665C` | 次级文字 |
| ink300   | `#A49A8C` | 弱化文字 / 编号 |
| line     | `#DDD4C5` | 墨线 / 缝线 |
| stamp    | `#221D18` | 印章墨色（全黑） |

## 页面映射（23 页，序号对应路由）

| # | Vue 路由 | Vue 页面 | iOS 控制器 | 需要访问 | 关键功能 |
|---|---|---|---|---|---|
| 01 | /login | LoginView | LoginController | 公开 | 账密登记、演示账号快捷填入、角色化首页入口 |
| 02 | /dashboard | DashboardView | DashboardController | 登录 | **角色化首页**：管理员→账户/系统管理为主；教师/职工→校园日常+待我审批；学生→今日课程/待办/通知/活动 |
| 03 | /service | ServiceCenter | ServiceController | 登录 | 校园事务清单（编号目录式）+ 发起 |
| 04 | /service/apply/:id | ServiceApply | ServiceApplyController | 登录 | 事务申请表（登记凭证卡） |
| 05 | /application/my | MyApplications | MyApplicationsController | 登录 | 我的申请列表（按状态印章：审批中/已办结/已驳回） |
| 06 | /application/detail/:id | ApplicationDetail | ApplicationDetailController | 登录 | 申请详情 |
| 07 | /approval | ApprovalCenter | ApprovalController | 教师/职工/管理员 | 三卷：待我审批 / 我已审批 / 我发起的 |
| 08 | /reservation | ReservationList | ReservationController | 登录 | 场地预约列表 |
| 09 | /reservation/detail/:id | ReservationDetail | ReservationDetailController | 登录 | 场地详情 + 预约 |
| 10 | /repair | RepairList | RepairController | 登录 | 校园报修列表 |
| 11 | /repair/detail/:id | RepairDetail | RepairDetailController | 登录 | 报修详情 |
| 12 | /repair/my | MyRepairs | MyRepairsController | 登录 | 我的报修 |
| 13 | /activity | ActivityList | ActivityController | 登录 | 校园活动列表（进度墨线） |
| 14 | /activity/detail/:id | ActivityDetail | ActivityDetailController | 登录 | 活动详情 + 报名 |
| 15 | /notice | NoticeList | NoticeController | 登录 | 校园资讯列表 |
| 16 | /notice/detail/:id | NoticeDetail | NoticeDetailController | 登录 | 通知详情 |
| 17 | /message | MessageCenter | MessageController | 登录 | 消息中心 |
| 18 | /ai | AICopilot | AIController | 登录 | AI 校园助手 |
| 19 | /task | TaskCenter | TaskController | 登录 | 任务中心 |
| 20 | /analytics | DataAnalytics | AnalyticsController | 管理员/超管 | 数据中心（宏观指标 + 柱状/环形图，墨阶配色） |
| 21 | /system | SystemManagement | SystemController | 超管 | 系统管理：模块入口（用户管理可进入，其余待建档） |
| 22 | /system/users | SystemUsers | SystemUsersController | 超管 | 用户管理（关键词+分页，角色章「教师/职工」） |
| 23 | 404 | NotFound | NotFoundController | 公开 | 该卷宗不在档 |

## 角色映射（= 后端）

| 角色码 | 名称 | 说明 |
|---|---|---|
| STUDENT    | 学生 | 校园日常 |
| TEACHER    | 教师 | 审批等常规职能 |
| COUNSELOR  | 职工 | 教师职能 + 发布通知/管理他人活动 |
| ADMIN      | 管理员 | 数据中心等 |
| SUPER_ADMIN| 超级管理员 | 系统管理/用户管理全权 |

## 验收口径
- 每个页面仅用黑白墨阶，无彩色渐变 / 彩色圆点 / emoji 图标。
- 列表项用等宽编号 `REC·001`，状态用「墨章」（实章=已办结、线章=审批中/驳回）。
- 动画只用 transform/opacity，150–300ms，并尊重 `prefers-reduced-motion`。
- `XcodeGen`：`xcodegen generate` 后打开 `CampusOne.xcodeproj` 即可编译。