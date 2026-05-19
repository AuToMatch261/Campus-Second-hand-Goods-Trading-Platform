package com.campus.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "账号", example = "alice")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "密码", example = "p@ss1234")
    @NotBlank(message = "密码不能为空")
    private String password;
}
