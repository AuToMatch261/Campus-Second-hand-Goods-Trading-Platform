# 项目说明

全栈校园二手物品交易平台应用，前端采用 Vue 3，后端采用 Spring Boot + Spring Cloud 微服务架构。

## 目录结构

my-project/
├── frontend/                       ← Vue 项目（不变）
├── backend/                        ← Maven 父工程
│   ├── pom.xml                     ← 父 POM，统一管理版本
│   ├── common/                     ← 公共模块（DTO、工具类、异常）
│   ├── gateway/                    ← Spring Cloud Gateway
│   ├── user-service/               ← 用户服务
│   ├── order-service/              ← 订单服务
│   └── xx-service/                ← 其他业务服务
├── docker/                         ← Nacos、MySQL、Redis 的 docker-compose
├── deploy/                         ← Nginx 配置、部署脚本
└── CLAUDE.md

## 后端模块

- backend/gateway/: API 网关服务，统一路由、鉴权、跨域处理
- backend/auth-service/: 认证服务，负责登录、注册、Token 生成与校验
- backend/user-service/: 用户服务，负责用户信息、个人资料管理
- backend/product-service/: 商品服务，负责商品发布、查询、分类、搜索
- backend/order-service/: 订单服务，负责交易订单、状态流转
- backend/message-service/: 消息服务，负责站内消息、通知、私信
- backend/common/: 公共模块，封装通用响应、异常处理、工具类、基础实体等

## 技术栈

### 前端

- Vue 3
- Vite
- TypeScript
- Pinia
- Axios

### 后端

- Spring Boot
- Spring Cloud
- Spring Cloud Gateway
- OpenFeign
- Nacos
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Elasticsearch

## 开发约定

- 前端包管理使用 pnpm
- 后端使用 Maven 管理依赖
- 前端代码使用 TypeScript
- 后端代码使用 Java
- 前后端通过 RESTful API 进行通信
- 接口文档使用 Knife4j / Swagger
- 提交信息使用中文

## 验证命令

- 前端构建：cd frontend && pnpm build
- 前端类型检查：cd frontend && pnpm type-check
- 后端构建：cd backend && mvn clean package
- 后端测试：cd backend && mvn test

## 接口契约同步（OpenAPI ↔ TypeScript）

后端：每个业务服务自带 `knife4j-openapi3-jakarta-spring-boot-starter`，启动后在 `http://localhost:<port>/v3/api-docs` 暴露 OpenAPI 3 JSON、`/doc.html` 暴露 Knife4j UI。

网关：装了 `knife4j-gateway-spring-boot-starter`，从 Nacos 自动发现服务，统一聚合：

- 文档 UI：<http://localhost:8080/doc.html>
- 单服务 OpenAPI：`http://localhost:8080/<service-id>/v3/api-docs`
  （如 `http://localhost:8080/auth-service/v3/api-docs`）

前端：`src/api/schema/<service>.d.ts` 是 `openapi-typescript` 从上面这些 URL 生成的契约文件，**入库**。

```powershell
cd frontend
pnpm sync-api              # 一次同步全部 5 个服务
pnpm sync-api:auth         # 也可单独同步一个
```

工作流：

1. 后端改 Controller / DTO → 重启对应服务（或热部署）
2. 前端 `pnpm sync-api` 拉最新类型
3. `git diff src/api/schema/` 看变化，编译期就能发现破坏性改动
4. 提交「后端接口变更 + 前端类型同步」作为一个 PR，避免漂移

## RabbitMQ 异步事件

订单状态变化通过 RabbitMQ 解耦商品上下架和系统通知，避免在订单事务内做远程调用。

- 中间件：`docker-compose` 里的 `rabbitmq:3.13-management`，管理 UI `http://localhost:15672`（默认账号 `campus / campus123`）
- Exchange：`campus.order.events`（topic）；DLX：`campus.dlx`
- 路由键：`order.created` / `order.confirmed` / `order.cancelled`
- 生产者：`order-service` 在事务 `AFTER_COMMIT` 后发布 `OrderEvent`（见 `OrderEventForwarder`），事务回滚则不发
- 消费者：
  - `product-service` 监听 `order.cancelled` → `tryRelist`（幂等：商品已上架直接返回 true）
  - `message-service` 监听 `order.#` → 写 `t_notification`（按 `(event_id, user_id)` 去重）

> **超售防线仍是同步的**：下单时 order-service 通过 Feign 调 `/internal/product/{id}/sold` 做 CAS 锁定状态，没改成事件——超售只能同步拦截。
> `relist` 已下线 Feign 端点，仅由事件触发；如需手动恢复上架走 DB 或加管理后台。

事件 DTO 在 `common/mq/event/OrderEvent`，路由键常量在 `common/mq/RabbitMqConstants`。新增事件类型时先在这里登记，再同步更新生产者/消费者两侧。

## Elasticsearch 搜索

商品列表搜索从 MySQL `LIKE` 改成 ES，支持中文分词、按价格/时间/浏览量排序。

- 中间件：`docker-compose` 里的 `elasticsearch` 服务用自定义 Dockerfile（`docker/elasticsearch/Dockerfile`）安装 `analysis-ik` 插件，8.13.4 单节点；端口 `9200`
- 索引：`products`（映射文件 `product-service/src/main/resources/es/product-index.json`，启动时 `ProductIndexInitializer` 按需创建；不存在就建，已存在就跳过，不覆盖现有 mapping）
- 字段分词：`title`/`description` 用 `ik_max_word` 索引、`ik_smart` 搜索；`price` 用 `scaled_float(100)`；`category` / `status` / `sellerId` 用精确匹配

数据同步走 RabbitMQ：

- Exchange：`campus.product.events`（topic）；队列 `search.product.events`
- product-service 在 publish / update / offShelf / softDelete / markSold / tryRelist 后通过 `ApplicationEventPublisher` 发本地 `ProductEvent`；`ProductEventForwarder` 在 `AFTER_COMMIT` 后投递 MQ
- product-service 自己起 `ProductEventListener` 消费，按 DB 当前状态把 doc 写到 ES（UPSERTED）或删除（DELETED）
- `viewCount` 高频写不发事件，由 `reindex` 兜底

运维：

- 重建索引：`POST /api/product/internal/product/reindex?purge=true`（仅内部调用；purge=true 会先删索引再按 mapping 重建）
- ES 不可用时，`ProductSearchService.search` 返回空结果而非 500，避免商品列表 UI 全挂

环境变量：`ES_URIS`（默认 `http://127.0.0.1:9200`）、`ES_PRODUCTS_INDEX`（默认 `products`）。



┌─────────────────────────────────────────────────────────────────┐
│                    阶段 0: 准备                                  │
│                                                                  │
│  手动操作（不用 Claude Code）:                                   │
│  • 安装 Claude Code: npm install -g @anthropic-ai/claude-code   │
│  • 创建项目根目录 + git init                                     │
│  • 手写最简 CLAUDE.md（项目目标 + 技术栈 + 目录约定）            │
│  • 首次 git commit                                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              阶段 1: 中间件环境（Docker Compose）                │
│                                                                  │
│  提示 Claude Code 生成 docker/docker-compose.yml:               │
│  • MySQL 8.0                                                     │
│  • Redis 7                                                       │
│  • Nacos 2.x（注册中心 + 配置中心）                              │
│  • Sentinel Dashboard（可选）                                    │
│                                                                  │
│  验证: docker compose up -d，浏览器访问 Nacos 控制台             │
│  Commit: "chore: add docker compose for middleware"             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│            阶段 2: 后端脚手架（Maven 多模块 + SCA）              │
│                                                                  │
│  关键提示: 让 Claude Code 先用 WebFetch 查官方版本兼容矩阵       │
│           再生成 pom.xml                                         │
│                                                                  │
│  生成结构:                                                       │
│    backend/                                                      │
│    ├── pom.xml（父 POM，dependencyManagement 统一版本）         │
│    ├── app-bootstrap/（启动模块，@SpringBootApplication）       │
│    ├── common/（Result、异常、工具类）                          │
│    └── module-xxx/（业务模块占位，先空着）                      │
│                                                                  │
│  配置:                                                           │
│  • application.yml（连 Nacos、连 MySQL、连 Redis）              │
│  • bootstrap.yml（Nacos 配置中心引导）                          │
│                                                                  │
│  验证: mvn clean compile 通过，启动空 Spring Boot 能注册到 Nacos │
│  Commit: "feat(backend): scaffold maven multi-module"           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              阶段 3: 前端脚手架（Vue 3 + Vite）                  │
│                                                                  │
│  让 Claude Code 用官方脚手架（不要手写）:                        │
│    pnpm create vite frontend -- --template vue-ts                │
│                                                                  │
│  补充安装:                                                       │
│  • Pinia（状态管理）                                            │
│  • Vue Router                                                    │
│  • Axios                                                         │
│  • UI 库（Element Plus / Ant Design Vue / Naive UI）            │
│  • openapi-typescript（同步后端类型）                           │
│                                                                  │
│  配置 vite.config.ts: dev server proxy /api → localhost:8080    │
│  配置 axios baseURL: 用环境变量 + 相对路径 /api                  │
│                                                                  │
│  验证: pnpm dev 起来，pnpm build 通过                            │
│  Commit: "feat(frontend): scaffold vue3 + vite"                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│             阶段 4: 契约工具链（OpenAPI 自动同步）               │
│                                                                  │
│  后端加 springdoc-openapi-starter-webmvc-ui 依赖                 │
│  → 自动暴露 /v3/api-docs 和 /swagger-ui.html                     │
│                                                                  │
│  前端 package.json 加 scripts:                                   │
│    "sync-api": "openapi-typescript                               │
│                 http://localhost:8080/v3/api-docs                │
│                 -o src/api/schema.d.ts"                          │
│                                                                  │
│  CLAUDE.md 写明: 后端改接口 → 前端 pnpm sync-api 同步类型        │
│                                                                  │
│  Commit: "chore: setup openapi type sync"                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
       ┌──────────────────────────────────────────────────┐
       │  此时基础设施全部就绪，进入功能迭代循环          │
       │  下面这个循环对每个业务功能重复执行              │
       └──────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         阶段 5: 垂直切片循环（每个功能重复一次）                 │
│                                                                  │
│  ┌─ 5.1 数据库 ─────────────────────────────────────┐           │
│  │ • 设计表结构（直接写 SQL 或让 Claude Code 写）   │           │
│  │ • 用 MyBatis-Plus FastAutoGenerator 一键生成    │           │
│  │   Entity + Mapper + Service 骨架                │           │
│  │ • Commit: "feat(db): add xxx table"             │           │
│  └──────────────────────────────────────────────────┘           │
│                          ↓                                       │
│  ┌─ 5.2 后端 Service 层 ────────────────────────────┐           │
│  │ • 写业务逻辑（不碰 HTTP）                        │           │
│  │ • 写 JUnit + Mockito 单元测试                    │           │
│  │ • 跑测试全绿                                     │           │
│  │ • Commit: "feat: xxx service layer"             │           │
│  └──────────────────────────────────────────────────┘           │
│                          ↓                                       │
│  ┌─ 5.3 后端 Controller 层 ─────────────────────────┐           │
│  │ • 写 @RestController，入参 DTO，返回 VO          │           │
│  │ • 加 Swagger 注解（@Operation、@Schema）         │           │
│  │ • 加全局异常处理（如果还没有）                   │           │
│  │ • 用 curl 手动验证每个端点                       │           │
│  │ • Commit: "feat: xxx api"                       │           │
│  └──────────────────────────────────────────────────┘           │
│                          ↓                                       │
│  ┌─ 5.4 前端类型同步 ───────────────────────────────┐           │
│  │ • pnpm sync-api 拉取最新 OpenAPI 类型           │           │
│  │ • Commit: "chore: sync api types"               │           │
│  └──────────────────────────────────────────────────┘           │
│                          ↓                                       │
│  ┌─ 5.5 前端 API + Store 层 ────────────────────────┐           │
│  │ • src/api/xxx.ts 封装 axios 调用                │           │
│  │ • src/stores/xxx.ts 写 Pinia store              │           │
│  │ • Commit: "feat(fe): xxx api & store"           │           │
│  └──────────────────────────────────────────────────┘           │
│                          ↓                                       │
│  ┌─ 5.6 前端组件 + 页面 ────────────────────────────┐           │
│  │ • 组件不直接访问 store/API（通过 props/emit）   │           │
│  │ • 页面（views/）是唯一访问 store 的层           │           │
│  │ • 浏览器手动验证：打开页面 → 点击 → 看数据       │           │
│  │ • Commit: "feat(fe): xxx page"                  │           │
│  └──────────────────────────────────────────────────┘           │
│                          ↓                                       │
│  ┌─ 5.7 端到端验证 ─────────────────────────────────┐           │
│  │ • 启动前后端，从 UI 跑通完整流程                 │           │
│  │ • 不行就 git reset 重做，不要打补丁              │           │
│  │ • /clear 清理上下文，进入下一个切片              │           │
│  └──────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                ┌────────────────────────────┐
                │  返回 5.1 做下一个功能      │
                │  直到所有功能完成           │
                └────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  阶段 6: 部署上线                                │
│                                                                  │
│  让 Claude Code 生成 deploy/ 目录:                              │
│  • Dockerfile（后端，多阶段构建）                                │
│  • nginx.conf（前端静态资源 + /api 反向代理）                   │
│  • docker-compose.prod.yml（生产环境编排）                      │
│  • deploy.sh（一键部署脚本）                                    │
│  • README.md（首次上线步骤、HTTPS 配置）                        │
│                                                                  │
│  Commit: "chore: production deploy config"                      │
└─────────────────────────────────────────────────────────────────┘