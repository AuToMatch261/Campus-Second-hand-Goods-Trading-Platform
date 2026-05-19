# Backend - 校园二手交易平台

Spring Boot 3.3.4 / Spring Cloud 2023.0.3 / Spring Cloud Alibaba 2023.0.3.4 / JDK 21。

## 模块

| 模块            | 端口 | 说明                                  |
| --------------- | ---- | ------------------------------------- |
| common          | -    | 通用响应 / 异常 / 工具类（jar 库）    |
| gateway         | 8080 | API 网关：路由、鉴权、跨域            |
| auth-service    | 8081 | 登录、注册、Token                     |
| user-service    | 8082 | 用户信息、个人资料                    |
| product-service | 8083 | 商品发布、查询、分类、搜索            |
| order-service   | 8084 | 交易订单、状态流转                    |
| message-service | 8085 | 站内消息、通知、私信                  |

## 前置

- JDK 21
- Maven 3.9+
- 已经按 `docker/README.md` 起好 MySQL / Redis / Nacos

## 构建

```powershell
# 项目根目录
cd backend
mvn -DskipTests clean package
```

`common` 是其它服务的依赖项，父工程的反应堆构建会自动处理顺序。

## Nacos 准备

启动业务服务之前，需要先在 Nacos 控制台准备 1 份共享配置：

1. 浏览器打开 <http://localhost:8848/nacos>（账号 / 密码：nacos / nacos）
2. 进入 **配置管理 → 配置列表**，确认命名空间 `public`、分组 `CAMPUS_GROUP`
3. 新建配置：
   - **Data ID**：`application-shared.yaml`
   - **Group**：`CAMPUS_GROUP`
   - **格式**：YAML
   - **内容**：见下方「application-shared.yaml 示例」

### application-shared.yaml 示例

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/campus?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root123
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: redis123
      timeout: 3000ms

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: change-me-to-a-strong-random-32-byte-secret
  expire-minutes: 120

logging:
  level:
    com.campus: debug
```

> 各服务的 `bootstrap.yml` 已配置 `shared-configs: application-shared.yaml`，启动时会自动拉取这份配置；如需服务专属配置，再在 Nacos 上新建 `<service-name>-dev.yaml`（如 `auth-service-dev.yaml`），Nacos 客户端默认会按 `${spring.application.name}-${profile}.${file-extension}` 加载。

## 启动顺序

1. `docker compose up -d`（确保 MySQL、Redis、Nacos 全部 healthy）
2. 在 Nacos 上完成上面那份 `application-shared.yaml`
3. 在 IDE 中分别运行：
   - `GatewayApplication`
   - `AuthApplication`
   - `UserApplication`
   - `ProductApplication`
   - `OrderApplication`
   - `MessageApplication`
4. 任一服务启动后回到 Nacos 控制台 → 服务管理 → 服务列表，可看到注册成功

## 验证

```powershell
# 直接打业务服务
curl http://localhost:8081/health/ping
# 走网关
curl http://localhost:8080/api/auth/health/ping
curl http://localhost:8080/api/user/health/ping
curl http://localhost:8080/api/product/health/ping
curl http://localhost:8080/api/order/health/ping
curl http://localhost:8080/api/message/health/ping
```

均应返回 `{"code":0,"message":"操作成功","data":"xxx-service pong"}`。

Knife4j 文档（各业务服务独立暴露）：

- <http://localhost:8081/doc.html>
- <http://localhost:8082/doc.html>
- ...
