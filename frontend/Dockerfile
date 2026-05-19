# syntax=docker/dockerfile:1.6
# ----- Build stage -----
FROM node:20-alpine AS builder

WORKDIR /app

# pnpm 通过 corepack 启用
RUN corepack enable

COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile || pnpm install

COPY . .

# 生产构建：API 走相对路径 /api，由 nginx 反代到 gateway
ENV VITE_API_BASE_URL=/api
RUN pnpm build

# ----- Runtime stage -----
FROM nginx:1.27-alpine AS runtime

# 主 nginx 配置由 deploy/nginx/nginx.conf 注入；这里只放静态资源
COPY --from=builder /app/dist /usr/share/nginx/html

# 给容器内自带的 default.conf 一份兜底站点配置（生产部署会用 deploy/ 下的 mount 覆盖）
RUN rm /etc/nginx/conf.d/default.conf
COPY <<'EOF' /etc/nginx/conf.d/default.conf
server {
    listen 80 default_server;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

EXPOSE 80
