# Frontend - 校园二手交易平台

Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios。

## 技术要点

- **状态管理**：Pinia + `pinia-plugin-persistedstate`（token、用户信息持久化到 localStorage）
- **UI 库**：Element Plus，通过 `unplugin-auto-import` + `unplugin-vue-components` 按需自动导入，无需在每个文件手写 import
- **路由**：Vue Router 4，全局前置守卫做登录拦截
- **请求库**：Axios 实例 `src/api/request.ts`，自动带 token、统一处理 `Result<T>` 响应、401 自动登出
- **接口契约**：`pnpm sync-api` 拉取后端 OpenAPI 生成 `src/api/schema.d.ts`
- **别名**：`@` → `src/`

## 前置

```powershell
# 一次性：安装 pnpm
npm install -g pnpm
```

确认 Node >= 20（项目使用 Vite 5 + TS 5.6）。

## 安装与启动

```powershell
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器（默认 http://localhost:5173）
pnpm dev

# 类型检查
pnpm type-check

# 生产构建
pnpm build

# 本地预览构建产物
pnpm preview
```

开发服务器会通过 `vite.config.ts` 的 proxy 把 `/api/**` 请求转发到 `http://localhost:8080`（网关）。该地址可用 `.env.development` 里的 `VITE_API_PROXY_TARGET` 覆盖。

## 同步后端类型

后端跑起来后：

```powershell
pnpm sync-api
```

会从 `http://localhost:8080/v3/api-docs` 拉取 OpenAPI 并生成 `src/api/schema.d.ts`，前端调用接口时用：

```ts
import type { components } from '@/api/schema'
type LoginResp = components['schemas']['LoginResult']
```

> 网关聚合 OpenAPI 的路由已在 `gateway/application.yml` 留了 `/v3/api-docs/{service}` 的转发；如先临时使用单个服务文档，可改成 `http://localhost:8081/v3/api-docs`。

## 目录结构

```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig*.json
├── .env.development        # VITE_API_BASE_URL = /api
├── .env.production
└── src/
    ├── main.ts             # 入口：装载 Pinia、Router、Element Plus 样式
    ├── App.vue
    ├── styles/             # 全局样式
    ├── router/             # 路由 + 守卫
    ├── stores/             # Pinia store（user 等）
    ├── api/                # 请求封装 + 业务接口 + schema.d.ts
    ├── views/              # 页面（唯一访问 store 的层）
    ├── components/         # 业务组件（通过 props/emit，不直接访问 store）
    └── assets/             # 静态资源
```

## 开发约定

- 组件不直接访问 store/API，通过 `props` / `emit` 通信
- `views/` 是唯一访问 store 的层
- 接口调用统一走 `@/api/*.ts`，不在组件里 import axios
- 后端改了接口 → 先 `pnpm sync-api` → 再写前端调用
