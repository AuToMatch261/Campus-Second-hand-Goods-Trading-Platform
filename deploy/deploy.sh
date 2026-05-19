#!/usr/bin/env bash
# 一键部署脚本（Linux / macOS）。
# Windows 请用同目录下的 deploy.ps1。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.prod.yml"
ENV_FILE="${SCRIPT_DIR}/.env"

green() { printf '\033[32m%s\033[0m\n' "$*"; }
red()   { printf '\033[31m%s\033[0m\n' "$*" >&2; }
yellow(){ printf '\033[33m%s\033[0m\n' "$*"; }

# ----- 前置检查 -----
command -v docker >/dev/null 2>&1 || { red "缺少 docker"; exit 1; }
docker compose version >/dev/null 2>&1 || { red "缺少 docker compose plugin（>= v2）"; exit 1; }

if [[ ! -f "${ENV_FILE}" ]]; then
    red ".env 不存在；请先 cp .env.example .env 并填实际密码"
    exit 1
fi

# 检查密码是否还是占位符
if grep -E "please-change-me|change-me-to" "${ENV_FILE}" >/dev/null; then
    red "${ENV_FILE} 中仍有 'please-change-me' 占位符，请改成真实密码后再部署"
    exit 1
fi

# ----- compose 起服务 -----
green ">>> 构建镜像（首次会拉基础镜像 + 安装 IK 插件，约 5-10 分钟）"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" build

green ">>> 启动中间件"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d \
    mysql redis nacos rabbitmq elasticsearch

yellow ">>> 等待中间件 healthcheck 通过（最多 3 分钟）"
deadline=$(( $(date +%s) + 180 ))
while :; do
    unhealthy=$(docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps \
        --format json | grep -oE '"Health":"[^"]+"' | grep -v 'healthy' || true)
    if [[ -z "${unhealthy}" ]]; then
        green ">>> 中间件全部 healthy"
        break
    fi
    if (( $(date +%s) > deadline )); then
        red ">>> 中间件未在 3 分钟内 healthy，请用 docker compose ps 查看"
        exit 1
    fi
    sleep 5
done

yellow ">>> 提醒：首次部署需在 Nacos 控制台导入 deploy/nacos-config/application-shared.yaml"
yellow "    控制台：http://127.0.0.1:8848/nacos  Group: CAMPUS_GROUP  Data ID: application-shared.yaml"
read -r -p "确认 Nacos 共享配置已导入？[y/N] " ans
if [[ "${ans}" != "y" && "${ans}" != "Y" ]]; then
    red "中止；请导入后重新执行 ./deploy.sh"
    exit 1
fi

green ">>> 启动业务服务和前端"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d

green ">>> 全部服务状态"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

cat <<EOF

$(green '部署完成')。下一步：
  • 浏览器打开 http://<服务器IP>/  访问前端
  • 浏览器打开 http://<服务器IP>/doc.html  访问 Knife4j 聚合文档
  • 首次部署需要回填 ES 商品索引：
      docker exec campus-product curl -X POST 'http://127.0.0.1:8083/internal/product/reindex?purge=true'

查看日志：docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f <service>
EOF
