# 一键部署脚本（Windows PowerShell 版本，与 deploy.sh 等价）
$ErrorActionPreference = 'Stop'

$ScriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$ComposeFile = Join-Path $ScriptDir 'docker-compose.prod.yml'
$EnvFile     = Join-Path $ScriptDir '.env'

function Green ($msg)  { Write-Host $msg -ForegroundColor Green }
function Red ($msg)    { Write-Host $msg -ForegroundColor Red }
function Yellow ($msg) { Write-Host $msg -ForegroundColor Yellow }

# ----- 前置检查 -----
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Red '缺少 docker'; exit 1
}
try { docker compose version | Out-Null } catch {
    Red '缺少 docker compose plugin（>= v2）'; exit 1
}

if (-not (Test-Path $EnvFile)) {
    Red '.env 不存在；请先 cp .env.example .env 并填实际密码'
    exit 1
}

if (Select-String -Path $EnvFile -Pattern 'please-change-me|change-me-to' -Quiet) {
    Red "$EnvFile 中仍有 'please-change-me' 占位符，请改成真实密码后再部署"
    exit 1
}

# ----- compose 起服务 -----
Green '>>> 构建镜像（首次会拉基础镜像 + 安装 IK 插件，约 5-10 分钟）'
docker compose -f $ComposeFile --env-file $EnvFile build

Green '>>> 启动中间件'
docker compose -f $ComposeFile --env-file $EnvFile up -d mysql redis nacos rabbitmq elasticsearch

Yellow '>>> 等待中间件 healthcheck 通过（最多 3 分钟）'
$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    $statuses = docker compose -f $ComposeFile --env-file $EnvFile ps --format json |
        ForEach-Object { $_ | ConvertFrom-Json } |
        Where-Object { $_.Health -and $_.Health -ne 'healthy' }
    if (-not $statuses) {
        Green '>>> 中间件全部 healthy'
        break
    }
    if ((Get-Date) -gt $deadline) {
        Red '>>> 中间件未在 3 分钟内 healthy，请用 docker compose ps 查看'
        exit 1
    }
    Start-Sleep -Seconds 5
}

Yellow '>>> 提醒：首次部署需在 Nacos 控制台导入 deploy/nacos-config/application-shared.yaml'
Yellow '    控制台：http://127.0.0.1:8848/nacos  Group: CAMPUS_GROUP  Data ID: application-shared.yaml'
$ans = Read-Host '确认 Nacos 共享配置已导入？[y/N]'
if ($ans -notin @('y','Y')) {
    Red '中止；请导入后重新执行 .\deploy.ps1'
    exit 1
}

Green '>>> 启动业务服务和前端'
docker compose -f $ComposeFile --env-file $EnvFile up -d

Green '>>> 全部服务状态'
docker compose -f $ComposeFile --env-file $EnvFile ps

@"

部署完成。下一步：
  - 浏览器打开 http://<服务器IP>/        访问前端
  - 浏览器打开 http://<服务器IP>/doc.html 访问 Knife4j 聚合文档
  - 首次部署需要回填 ES 商品索引：
      docker exec campus-product curl -X POST 'http://127.0.0.1:8083/internal/product/reindex?purge=true'

查看日志：docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env logs -f <service>
"@ | Write-Host
