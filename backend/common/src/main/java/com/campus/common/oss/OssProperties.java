package com.campus.common.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 阿里云 OSS 配置。AK/Secret 必须通过环境变量传入,代码与配置文件中禁止写死。
 *
 * 推荐用 RAM 子账号 + 仅授予目标 bucket 的 PutObject/GetObject 权限。
 */
@Data
@ConfigurationProperties(prefix = "campus.oss")
public class OssProperties {

    /** 是否启用 OSS 自动装配,默认 false。仅需用 OSS 的服务设为 true。 */
    private boolean enabled = false;

    /** OSS endpoint,例如 https://oss-cn-beijing.aliyuncs.com */
    private String endpoint;

    /** AccessKey ID,通过 ALIYUN_OSS_AK 环境变量注入 */
    private String accessKeyId;

    /** AccessKey Secret,通过 ALIYUN_OSS_SECRET 环境变量注入 */
    private String accessKeySecret;

    /** Bucket 名称 */
    private String bucket;

    /** 自定义域名(可选,带 scheme)。为空则生成 https://{bucket}.{endpoint-host}/{key} */
    private String customDomain;

    /** 单文件最大字节数,默认 10 MB */
    private long maxFileSize = 10L * 1024 * 1024;

    /** 允许的扩展名(小写,不含点) */
    private List<String> allowedExtensions =
            List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 允许的 Content-Type 前缀,例如 image/ */
    private List<String> allowedMimePrefixes = List.of("image/");
}
