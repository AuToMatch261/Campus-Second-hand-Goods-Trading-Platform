package com.campus.product.controller;

import com.campus.common.oss.OssTemplate;
import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 商品图片上传接口。
 *
 * 网关路由:POST /api/product/me/products/upload
 *   - 经 StripPrefix=2 后到达 /me/products/upload
 *   - 需登录(网关 JWT 校验 + X-User-Id 头)
 *
 * 注意:product-service 默认启用 OSS(application.yml 里 campus.oss.enabled=true)。
 * 若启动前未注入 ALIYUN_OSS_AK / ALIYUN_OSS_SECRET,OssAutoConfiguration 会让进程
 * 启动失败 + 清晰错误提示,这里直接 fail-fast,不再加 @ConditionalOnBean(那个注解
 * 在 component-scan 阶段不评估,组合 @Configuration 反而会让本 controller 失去
 * @RequestMapping 注册,引发 405 misleading 错误)。
 */
@Tag(name = "my-product-upload", description = "商品图片上传(需登录)")
@RestController
@RequestMapping("/me/products")
@RequiredArgsConstructor
public class ProductUploadController {

    private final OssTemplate ossTemplate;

    @Operation(summary = "上传单张商品图片,返回 OSS 公开 URL")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> upload(@CurrentUser Long userId,
                                              @RequestPart("file") MultipartFile file) {
        // userId 仅用于校验登录态,目录前缀不带 userId 是为了便于 CDN 缓存共享
        String url = ossTemplate.uploadImage(file, "products");
        return Result.ok(Map.of("url", url));
    }
}
