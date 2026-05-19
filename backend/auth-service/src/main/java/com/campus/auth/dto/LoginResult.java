package com.campus.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录返回")
public class LoginResult {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token 有效期（分钟）")
    private long expireMinutes;

    @Schema(description = "当前用户")
    private UserVO user;
}
