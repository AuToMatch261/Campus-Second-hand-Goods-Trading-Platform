package com.campus.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "发送消息请求")
public class SendMessageRequest {

    @Schema(description = "接收者用户 ID")
    @NotNull(message = "接收者不能为空")
    private Long toUserId;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "单条消息最长 2000 字符")
    private String content;

    @Schema(description = "关联的商品 ID（可选）")
    private Long relatedProductId;
}
