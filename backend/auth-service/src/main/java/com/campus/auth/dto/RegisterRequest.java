package com.campus.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "账号（4-20 位字母/数字/下划线）", example = "alice")
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "账号需为 4-20 位字母/数字/下划线")
    private String username;

    @Schema(description = "密码（6-32 位）", example = "p@ss1234")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @Schema(description = "昵称", example = "Alice")
    @Size(max = 50, message = "昵称最长 50 字符")
    private String nickname;
}
