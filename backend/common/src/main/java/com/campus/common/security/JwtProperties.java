package com.campus.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 密钥，长度至少 32 字节 */
    private String secret = "change-me-to-a-strong-random-32-byte-secret-please";

    /** Token 有效期（分钟） */
    private long expireMinutes = 120;

    /** 签发方 */
    private String issuer = "campus";
}
