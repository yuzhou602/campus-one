# CampusOne 开发与部署指南

本文说明如何运行静态演示、启动完整系统、执行验证，以及把本地开发配置转换为生产部署。

## 1. 选择运行模式

| 模式 | 用途 | 数据位置 | 是否需要后端 |
| --- | --- | --- | --- |
| GitHub Pages 演示 | 作品展示、答辩、公开浏览 | 浏览器 `localStorage` | 否 |
| Docker Compose | 本地完整联调 | MySQL、Redis、Docker Volume | 是 |
| 分别运行 | 前后端开发调试 | 本地 MySQL、Redis | 是 |
| 生产部署 | 多人真实使用 | 托管数据库、Redis、对象存储 | 是 |

## 2. 三步运行静态演示

### 第 1 步：安装依赖

```bash
cd campus-one-frontend
npm ci
```

### 第 2 步：构建演示

```bash
npm run build:demo
```

构建成功后，`dist/` 中包含 Hash 路由和相对资源路径的静态站点。

### 第 3 步：本地预览

```bash
npm run preview
```

打开命令输出的地址。使用 `student01`、`teacher01`、`counselor01`、`service01` 或 `admin` 登录，演示密码为 `demo123`。

你现在得到的是完整交互演示，不是真实多人环境。演示操作只影响当前浏览器。

## 3. 使用 Docker Compose 启动完整系统

### 前置条件

- Docker Engine 与 Docker Compose；
- 端口 `3000`、`8080`、`3306`、`6379` 未被占用；
- 生产之外的本地开发环境。

### 步骤

1. 复制环境变量模板。

   ```bash
   cp .env.example .env
   ```

   Windows PowerShell：

   ```powershell
   Copy-Item .env.example .env
   ```

2. 替换 `.env` 中所有 `replace-with-...` 占位符。`JWT_SECRET` 至少使用 32 字节随机值。

3. 构建并启动。

   ```bash
   docker compose up --build
   ```

4. 验证服务。

   - Web：`http://localhost:3000`
   - 健康检查：`http://localhost:8080/actuator/health`
   - Swagger：`http://localhost:8080/swagger-ui.html`

### 常见问题

**数据库初始化失败：** 如果日志显示表或索引重复，先确认是否复用了不兼容的旧数据卷。生产数据不能直接删除，应备份后按 Flyway 日志修复。

**后端无法连接 Redis：** 检查 Compose 中 `SPRING_DATA_REDIS_HOST=redis`，不要在容器内使用 `localhost`。

**浏览器提示跨域：** 把实际 Web 来源加入 `CORS_ORIGINS`，多个来源用英文逗号分隔。不要在允许凭据时配置任意来源。

**上传文件在重建后丢失：** 确认后端上传目录挂载到了 `uploads_data`；生产环境应使用对象存储。

## 4. 分别运行

### Web 前端

```bash
cd campus-one-frontend
npm ci
npm run dev
```

### Java 后端

需要 Java 21、MySQL 8 和 Redis 7，并设置 `DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`。

```bash
cd campus-one-backend
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
$env:JAVA_HOME='你的 JDK 21 目录'
.\mvnw.cmd spring-boot:run
```

## 5. 运行质量检查

```bash
# 后端全部测试
cd campus-one-backend
./mvnw test

# 前端类型检查与正式构建
cd ../campus-one-frontend
npm run build

# 前端演示模式构建
npm run build:demo
```

两个前端构建共用 `dist/` 输出目录，应按上述顺序串行执行；不要在同一个工作目录并行运行，否则清理输出目录时会产生文件占用冲突。

提交前还应执行：

```bash
git diff --check
git status --short
```

## 6. GitHub Pages 自动发布

`.github/workflows/pages.yml` 在 `main` 更新后构建演示模式并发布。部署完成后访问：

<https://yuzhou602.github.io/campus-one/>

Pages 只部署前端静态文件。不要把后端地址、数据库口令或生产 Token 写进演示构建。

## 7. 生产部署拓扑

```text
Internet
   │
HTTPS / WAF / Rate Limit
   │
Reverse Proxy
   ├── Vue static assets
   └── /api/v1 ──► Spring Boot replicas
                         │
              ┌──────────┼──────────┐
              │          │          │
          Managed      Managed    Object
           MySQL        Redis     Storage
```

上线前必须完成：

- 为 Web 和 API 配置 HTTPS 与固定域名；
- 使用托管 MySQL 和 Redis，禁止暴露数据库公网端口；
- 使用密钥管理或平台环境变量注入密码和 JWT 密钥；
- 将文件上传迁移到持久化对象存储，并校验附件归属；
- 关闭或保护生产 Swagger；
- 配置数据库备份、恢复演练和 Flyway 上线流程；
- 配置集中日志、运行指标、健康检查和告警；
- 设置反向代理请求体限制、连接超时和安全响应头；
- 准备隐私、数据保留、账号停用和审计策略。

## 8. iOS 构建

iOS 目录是 UIKit 源码。Windows 环境无法完成 Xcode 编译验证。

```bash
brew install xcodegen
cd campus-one-ios
xcodegen generate
open CampusOne.xcodeproj
```

在构建设置中配置 `API_BASE_URL`，并使用 HTTPS 生产 API。开发期若连接局域网 HTTP，需要按 Apple 平台规则配置网络权限；生产包不应保留宽泛的明文传输例外。

## 9. 发布检查清单

- [ ] 后端测试全部通过。
- [ ] 正式前端和演示前端均构建成功。
- [ ] Flyway 在生产数据副本上演练成功。
- [ ] 服务目录 ID 与 `/services` 返回一致。
- [ ] 学生、审批人、维修人员和管理员角色各完成一次主流程验收。
- [ ] 生产密钥未进入 Git 历史或构建产物。
- [ ] 数据库、附件和 Redis 数据具有可恢复备份。
- [ ] 发布后验证登录、申请、预约、报修、通知和健康检查。

## 相关文档

- [系统设计](SYSTEM_DESIGN.md)
- [业务流程与状态机](BUSINESS_WORKFLOWS.md)
- [API 与数据模型](API_AND_DATA_MODEL.md)
