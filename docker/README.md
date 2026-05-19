# 开发环境中间件

本目录使用 Docker Compose 一键启动开发所需的中间件：

| 服务  | 版本              | 宿主机端口             | 默认凭据                        |
| ----- | ----------------- | ---------------------- | ------------------------------- |
| MySQL | 8.0               | 3306                   | root / root123，初始库 `campus` |
| Redis | 7.2               | 6379                   | 密码 `redis123`                 |
| Nacos | 2.3.2（standalone）| 8848 / 9848 / 9849     | nacos / nacos                   |

数据持久化目录：`docker/data/`（已加入 `.gitignore`，请勿提交）。

## 前置条件

- Docker Desktop 4.x（含 Compose v2）
- Windows 用户开启 WSL2 后端
- 宿主机 3306 / 6379 / 8848 / 9848 / 9849 端口未被占用

## 启动

```bash
# 在仓库根目录
cd docker

# 后台启动全部服务
docker compose up -d

# 查看状态（等待全部 healthy）
docker compose ps
```

启动完成后可访问：

- Nacos 控制台：<http://localhost:8848/nacos>（账号/密码：nacos / nacos）
- MySQL：`mysql -h 127.0.0.1 -P 3306 -uroot -proot123`
- Redis：`redis-cli -h 127.0.0.1 -p 6379 -a redis123`

## 常用命令

```bash
# 查看日志
docker compose logs -f nacos
docker compose logs -f mysql
docker compose logs -f redis

# 仅重启某个服务
docker compose restart nacos

# 停止但保留数据
docker compose stop

# 停止并删除容器（数据卷在 ./data 下，依然保留）
docker compose down
```

## 重置（清空数据，回到初始状态）

> ⚠️ 以下操作会清空本地的 MySQL、Redis、Nacos 数据，仅用于开发环境。

```bash
# 1. 停止并移除容器、网络
docker compose down

# 2. 删除持久化数据目录
#    PowerShell:
Remove-Item -Recurse -Force .\data
#    Bash / WSL:
rm -rf ./data

# 3. 重新启动（容器初始化时会自动重建库表和数据目录）
docker compose up -d
```

仅重置单个服务：

```bash
# 例：只重置 MySQL
docker compose stop mysql
docker compose rm -f mysql
Remove-Item -Recurse -Force .\data\mysql   # 或 rm -rf ./data/mysql
docker compose up -d mysql
```

## 初始化 SQL

将需要在首次启动时执行的 SQL 文件放入 `docker/data/mysql/init/`，MySQL 容器在数据目录为空时会按文件名顺序执行其中的 `.sql` / `.sh`。已有数据后再放入不会重跑，需要先执行上面的「重置」流程。

## 接入后端

后端在 `application.yml` / Nacos 配置中按以下地址连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/campus?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root123
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: redis123
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos
```

容器之间互访请使用服务名 `mysql` / `redis` / `nacos` 作为主机名（同处于 `campus-net` 网络）。
