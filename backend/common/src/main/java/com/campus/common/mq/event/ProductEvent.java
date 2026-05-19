package com.campus.common.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品变更事件。
 * UPSERTED：发布 / 修改 / 上下架 / 售出，搜索端按 id 全量重建该 doc
 * DELETED：商品被逻辑删除，搜索端按 id 删除 doc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent implements Serializable {

    private String eventId;

    private Type type;

    private Long productId;

    private LocalDateTime occurredAt;

    public enum Type {
        UPSERTED, DELETED
    }
}
