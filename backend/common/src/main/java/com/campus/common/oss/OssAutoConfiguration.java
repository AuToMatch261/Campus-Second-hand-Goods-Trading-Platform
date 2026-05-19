package com.campus.common.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * OSS 自动装配。仅在 campus.oss.enabled=true 且 OSS SDK 在 classpath 时激活。
 *
 * 启动时校验 endpoint / accessKeyId / accessKeySecret / bucket 必填,
 * 任一为空直接抛 IllegalStateException 让应用启动失败,避免"看似启动成功但上传时炸"。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(OSS.class)
@ConditionalOnProperty(prefix = "campus.oss", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public OSS ossClient(OssProperties props) {
        requireNonBlank(props.getEndpoint(), "campus.oss.endpoint (或 ALIYUN_OSS_ENDPOINT)");
        requireNonBlank(props.getAccessKeyId(), "campus.oss.access-key-id (ALIYUN_OSS_AK 环境变量)");
        requireNonBlank(props.getAccessKeySecret(), "campus.oss.access-key-secret (ALIYUN_OSS_SECRET 环境变量)");
        requireNonBlank(props.getBucket(), "campus.oss.bucket (或 ALIYUN_OSS_BUCKET)");
        log.info("初始化 OSS 客户端 endpoint={} bucket={}", props.getEndpoint(), props.getBucket());
        return new OSSClientBuilder().build(
                props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
    }

    @Bean
    @ConditionalOnMissingBean
    public OssTemplate ossTemplate(OSS ossClient, OssProperties props) {
        return new OssTemplate(ossClient, props);
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "OSS 配置缺失: " + name + " 未设置。请检查环境变量或 application.yml");
        }
    }
}
