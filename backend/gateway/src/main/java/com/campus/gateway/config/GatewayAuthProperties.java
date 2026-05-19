package com.campus.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /** HS256 密钥，必须与 auth-service 的 jwt.secret 一致 */
    private String secret = "change-me-to-a-strong-random-32-byte-secret-please";

    /** 白名单（Ant 风格路径），不需要鉴权 */
    private List<String> whitelist = new ArrayList<>(List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/health/**",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/favicon.ico"
    ));
}
