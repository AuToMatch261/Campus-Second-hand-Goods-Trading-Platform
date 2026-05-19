package com.campus.common.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private String eventId;

    private Type type;

    private Long orderId;

    private Long productId;

    private String productTitle;

    private BigDecimal price;

    private Long buyerId;

    private Long sellerId;

    /** PENDING / COMPLETED / CANCELLED 等订单当前状态 */
    private String orderStatus;

    /** 取消事件用：买家 / 卖家 */
    private String cancelledBy;

    private LocalDateTime occurredAt;

    public enum Type {
        CREATED, CONFIRMED, CANCELLED
    }
}
