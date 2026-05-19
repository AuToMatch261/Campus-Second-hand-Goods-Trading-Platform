package com.campus.order.dto;

import com.campus.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "订单信息")
public class OrderVO {

    private Long id;

    private Long productId;

    private String productTitle;

    private String productImage;

    private BigDecimal price;

    private Long buyerId;

    private Long sellerId;

    private OrderStatus status;

    @Schema(description = "状态中文名")
    private String statusLabel;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;
}
