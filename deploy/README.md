# 部署指引

校园二手交易平台单机生产部署。前置假设：

- 一台 Linux 服务器（Ubuntu 22.04+ / CentOS 8+），≥ 4 核 8G 内存、≥ 40G 磁盘
- Docker 24+ 和 docker compose plugin v2
- 服务器有公网域名（HTTPS 部分需要）

## 目录约定

```
deploy/
├── docker-compose.prod.yml   # 生产编排
├── .env.example              # 环境变量模板（拷贝为 .env）
├── deploy.sh / deploy.ps1    # 一键部署
├── nginx/nginx.conf          # 前端 + 反代配置
├── nacos-config/             # 首次部署需导入 Nacos 的共享配置
└── README.md
```

## 一、首次部署步骤

### 1. 准备配置

```bash
cd deploy
cp .env.example .env
# 把 .env 里所有 please-change-me 占位符替换成真实密码：
#   MYSQL_ROOT_PASSWORD / REDIS_PASSWORD / NACOS_PASSWORD
#   NACOS_AUTH_TOKEN / RABBITMQ_PASSWORD / JWT_SECRET
# JWT_SECRET 必须 >= 32 字节，可用 `openssl rand -base64 48` 生成
```

同步修改 `nacos-config/application-shared.yaml` 里的 `spring.datasource.password` /
`spring.data.redis.password` / `jwt.secret` 三处，保持与 `.env` 一致（这份配置一会要
导入 Nacos，业务服务从 Nacos 读取，所以不能用 ${} 引用宿主 env）。

### 2. 启动中间件（不启业务，先建 Nacos 配置）

```bash
./deploy.sh
# 脚本会构建镜像 → 启动中间件 → 在中间件 healthy 后停下来等你导入 Nacos 配置
```

或者手动：

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d \
    mysql redis nacos rabbitmq elasticsearch
```

等大约 1-2 分钟 Nacos 起来，浏览器打开 `http://<服务器IP>:8848/nacos`：

- 默认账号：用 `.env` 里的 `NACOS_USERNAME` / `NACOS_PASSWORD`（首次启动会把这套凭据写入元数据库）
- 进入 **配置管理 → 配置列表**：
  - 命名空间：`public`（与 `.env` 中 `NACOS_NAMESPACE` 一致）
  - 点 「+」新建配置：
    - Data ID: `application-shared.yaml`
    - Group: `CAMPUS_GROUP`
    - 格式: `YAML`
    - 内容：把 `deploy/nacos-config/application-shared.yaml` 文件内容粘贴进去
  - 发布

> Nacos 默认密码只在首次启动时生效，导入配置后可在 **权限控制 → 用户列表** 改强密码。

### 3. 启动业务服务

回到 `deploy.sh` 的交互终端，回车 `y` 继续；或者重新跑：

```bash
./deploy.sh
```

`deploy.sh` 会拉起 gateway / auth / user / product / order / message / frontend 七个容器。
首次启动 Spring Boot 服务约 30-60 秒。

### 4. 验证

```bash
# 各容器全部 Up
docker compose -f docker-compose.prod.yml --env-file .env ps

# 前端
curl -I http://<服务器IP>/

# 网关聚合文档
curl http://<服务器IP>/doc.html

# 健康检查
curl http://<服务器IP>/api/auth/health/ok
```

### 5. 回填商品搜索索引（首次必做，否则广场是空的）

```bash
docker exec campus-product \
    curl -s -X POST 'http://127.0.0.1:8083/internal/product/reindex?purge=true'
```

返回写入的文档数即成功。

## 二、HTTPS 升级

推荐用 Let's Encrypt 的 certbot 在宿主机申请证书，把证书目录挂进 nginx 容器。

### 1. 装 certbot

```bash
sudo apt update && sudo apt install -y certbot
```

### 2. 临时停 80 端口 / 或用 webroot 验证

```bash
docker compose -f docker-compose.prod.yml --env-file .env stop frontend
sudo certbot certonly --standalone -d campus.example.com -m you@example.com --agree-tos -n
```

证书会写到 `/etc/letsencrypt/live/campus.example.com/`。

### 3. 打开 docker-compose.prod.yml 里 frontend 的 443 端口和证书挂载

```yaml
ports:
  - "${HTTP_PORT:-80}:80"
  - "${HTTPS_PORT:-443}:443"
volumes:
  - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
  - /etc/letsencrypt:/etc/letsencrypt:ro
```

### 4. 编辑 `deploy/nginx/nginx.conf`：

- 把 「HTTPS 服务块」那部分注释打开
- 把 `campus.example.com` 替换成你的域名
- 启用 80 → 443 永久跳转（已在配置文件里给了模板）

### 5. 重启

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d frontend
```

### 6. 续期

```bash
# 编辑 root 的 crontab
0 3 * * * certbot renew --quiet --post-hook "docker exec campus-frontend nginx -s reload"
```

## 三、日常操作

```bash
# 滚动重启某个服务（重新拉代码后）
docker compose -f docker-compose.prod.yml --env-file .env build product-service
docker compose -f docker-compose.prod.yml --env-file .env up -d product-service

# 看日志
docker compose -f docker-compose.prod.yml --env-file .env logs -f --tail=200 product-service

# 完整停掉
docker compose -f docker-compose.prod.yml --env-file .env down

# 完整删除（带卷，会丢数据）
docker compose -f docker-compose.prod.yml --env-file .env down -v
```

## 四、备份

需要持久化的数据卷：

| 卷名 | 内容 |
| --- | --- |
| `mysql-data` | 业务数据 |
| `redis-data` | 缓存（可不备） |
| `nacos-data` + `nacos-logs` | Nacos 元数据与日志 |
| `rabbitmq-data` | 队列状态 |
| `es-data` + `es-logs` | 搜索索引（丢了可用 reindex 接口重建） |

推荐 `mysql-data` 每天 dump 一次：

```bash
docker exec campus-mysql sh -c \
  'mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" campus' > campus-$(date +%F).sql
```

## 五、故障排查

| 现象 | 排查 |
| --- | --- |
| 服务起不来，日志显示连不上 Nacos | 检查 `.env` 中 `NACOS_USERNAME/PASSWORD` 与 Nacos 控制台是否一致；命名空间是否存在 |
| 业务服务报 `jwt.secret 长度必须 >= 32` | `application-shared.yaml` 里 `jwt.secret` 太短或没改 |
| 前端访问 502 | `docker compose ps` 看 gateway 是否 healthy；进 frontend 容器 `curl gateway:8080/actuator/health` |
| ES 启动失败，virtual memory areas 不足 | 在宿主机执行 `sudo sysctl -w vm.max_map_count=262144`，并写入 `/etc/sysctl.conf` |
| 商品列表空 | 执行第 5 步的 reindex；或检查 product-service 日志确认事件消费正常 |
