FROM docker.elastic.co/elasticsearch/elasticsearch:8.13.4

# 安装 IK 中文分词插件（infinilabs 维护的 medcl/elasticsearch-analysis-ik 后继版本）
ARG IK_VERSION=8.13.4
RUN bin/elasticsearch-plugin install --batch \
    "https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-${IK_VERSION}.zip"
