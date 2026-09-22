# CampusOne 智慧校园 · iOS 原生端

原生 **UIKit**（非 SwiftUI）。设计语言：**「校园档案 · 纸张与墨水」**（纯黑白、卡纸面、墨线、印章）。

## 与 Vue 端同步
- 功能 / 交互 / 设计语言与 `campus-one-frontend` 保持一致，详见 [MANIFEST.md](./MANIFEST.md)（23 页面映射 + 设计令牌 + 角色映射）。
- 后端接口统一 `/api/v1` 前缀，见 `Core/APIClient.swift`。

## 环境说明（重要）
当前工程在 Windows 环境产出，**没有 Xcode / Swift 工具链，无法在本机编译或运行**。交付物是可被 Xcode 直接装配、编译的原生源码工程。请在 macOS + Xcode 环境下构建：

```bash
# 1) 安装 XcodeGen（或手动建空 UIKit 工程后，把 CampusOne/ 目录拖入 target）
brew install xcodegen

# 2) 在 campus-one-ios 目录生成 .xcodeproj
cd campus-one-ios
xcodegen generate
open CampusOne.xcodeproj
```

## 结构与依赖
| 目录 | 内容 |
|---|---|
| `CampusOne/App` | 入口（AppDelegate/SceneDelegate/Info.plist/装配） |
| `CampusOne/Design` | 设计令牌与可复用组件（Theme、Components） |
| `CampusOne/Core` | 网络层、模型、会话、主容器（UITabBar 角色化） |
| `CampusOne/Features` | 23 个页面控制器（与 MANIFEST 对应） |

联网权限：请在 Signing & Capabilities 或 `Info.plist` 打开 **本地网络/明文 HTTP**（开发连后端时后台服务为 `http://<host>:8080`）。

## 端到端示例
- 首次进入 → `AppRouter` 判断有无 token → 无则 `LoginController`，有则 `MainTabController`。
- Access/Refresh Token 由 `AuthStore` 写入 iOS Keychain；用户名、显示名与角色等非敏感展示信息使用 UserDefaults 持久化。
- 首页 `DashboardController` 按角色渲染「管理视图 / 校园视图」。
