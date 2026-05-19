package com.campus.message.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationVO {

    private Long id;

    private String type;

    private String title;

    private String content;

    private String refType;

    private Long refId;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
