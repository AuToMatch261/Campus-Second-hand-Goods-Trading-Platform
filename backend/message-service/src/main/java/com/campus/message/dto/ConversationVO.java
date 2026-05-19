package com.campus.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "会话")
public class ConversationVO {

    private Long id;

    @Schema(description = "对端用户 ID（始终是「不是当前用户」的那一方）")
    private Long peerId;

    private Long lastMessageId;

    private String lastMessageText;

    private LocalDateTime lastMessageAt;

    @Schema(description = "当前用户在此会话的未读数")
    private Integer unread;
}
