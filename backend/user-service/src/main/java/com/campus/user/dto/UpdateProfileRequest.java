package com.campus.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新个人资料请求；只更新非 null 字段")
public class UpdateProfileRequest {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称最长 50 字符")
    private String nickname;

    @Schema(description = "头像 URL")
    @Size(max = 255, message = "头像 URL 最长 255 字符")
    private String avatar;

    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 字符")
    private String email;
}
