# syntax=docker/dockerfile:1.6
# ----- Build stage -----
FROM maven:3.9-eclipse-temurin-21 AS builder

ARG SERVICE_NAME
ENV MAVEN_OPTS="-Dmaven.test.skip=true -Dmaven.repo.local=/root/.m2/repository"

WORKDIR /workspace
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY message-service/pom.xml message-service/pom.xml

# 先下载依赖（带 cache mount，重复构建走缓存）
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -pl ${SERVICE_NAME} -am dependency:go-offline -DskipTests || true

COPY common common
COPY gateway gateway
COPY auth-service auth-service
COPY user-service user-service
COPY product-service product-service
COPY order-service order-service
COPY message-service message-service

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -pl ${SERVICE_NAME} -am package -DskipTests \
 && cp ${SERVICE_NAME}/target/*.jar /workspace/app.jar

# ----- Runtime stage -----
FROM eclipse-temurin:21-jre-jammy AS runtime

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME} \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Shanghai"

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1000 -g root campus

WORKDIR /app
COPY --from=builder /workspace/app.jar /app/app.jar
RUN chown -R campus:root /app

USER campus
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
