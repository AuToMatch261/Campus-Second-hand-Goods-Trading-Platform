package com.campus.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "消息")
public class MessageVO {

    private Long id;
    private Long conversationId;
    private Long fromId;
    private Long toId;
    private String content;
    private Long relatedProductId;
    private LocalDateTime createdAt;
}
