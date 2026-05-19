package com.campus.common.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * OSS 上传门面。屏蔽 SDK 细节,只暴露安全 / 业务相关的入口。
 *
 * 关键安全点:
 *   1. 文件名由服务端生成(UUID),客户端 filename 仅用于识别扩展名
 *   2. 扩展名 + Content-Type 双重白名单
 *   3. 单文件大小硬上限
 *   4. dirPrefix 做正则清洗,避免路径穿越
 */
@Slf4j
public class OssTemplate {

    private final OSS client;
    private final OssProperties props;

    public OssTemplate(OSS client, OssProperties props) {
        this.client = client;
        this.props = props;
    }

    /**
     * 上传图片,返回公开访问 URL。
     *
     * @param file      multipart 文件
     * @param dirPrefix 业务目录前缀,如 "products" / "avatars",会拼接 yyyy/MM/uuid.ext
     */
    public String uploadImage(MultipartFile file, String dirPrefix) {
        validate(file);
        String ext = extractExtension(file.getOriginalFilename());
        String key = buildKey(dirPrefix, ext);

        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentType(file.getContentType());
        meta.setCacheControl("public, max-age=31536000, immutable");

        try (InputStream in = file.getInputStream()) {
            client.putObject(props.getBucket(), key, in, meta);
        } catch (IOException e) {
            log.error("读取上传文件失败 key={}", key, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件读取失败");
        } catch (Exception e) {
            log.error("OSS 上传失败 key={}", key, e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片上传失败,请稍后重试");
        }
        return buildUrl(key);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件为空");
        }
        if (file.getSize() > props.getMaxFileSize()) {
            long mb = props.getMaxFileSize() / 1024 / 1024;
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件大小不能超过 " + mb + " MB");
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (ext == null || !props.getAllowedExtensions().contains(ext)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "仅支持图片格式: " + String.join("/", props.getAllowedExtensions()));
        }
        String ct = file.getContentType();
        boolean ctOk = ct != null && props.getAllowedMimePrefixes().stream().anyMatch(ct::startsWith);
        if (!ctOk) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件类型不被允许");
        }
    }

    private String buildKey(String prefix, String ext) {
        String safe = (prefix == null || prefix.isBlank())
                ? "uploads"
                : prefix.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safe.isBlank()) safe = "uploads";
        LocalDate today = LocalDate.now();
        return String.format(Locale.ROOT, "%s/%d/%02d/%s.%s",
                safe, today.getYear(), today.getMonthValue(),
                UUID.randomUUID().toString().replace("-", ""), ext);
    }

    private String buildUrl(String key) {
        if (props.getCustomDomain() != null && !props.getCustomDomain().isBlank()) {
            String base = props.getCustomDomain();
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            return base + "/" + key;
        }
        // 默认: https://{bucket}.{endpoint-host}/{key}
        String endpoint = props.getEndpoint();
        String scheme = endpoint.startsWith("http://") ? "http" : "https";
        String host = endpoint.replaceFirst("^https?://", "");
        return String.format("%s://%s.%s/%s", scheme, props.getBucket(), host, key);
    }

    private static String extractExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot >= filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
