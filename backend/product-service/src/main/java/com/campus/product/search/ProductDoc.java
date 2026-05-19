package com.campus.product.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ES 中 products 索引的文档。字段映射由 product-index.json 显式定义，
 * 不依赖运行时自动推断，避免类型漂移。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDoc {

    private Long id;

    private Long sellerId;

    private String title;

    private String description;

    private Integer category;

    private Integer status;

    private BigDecimal price;

    private Integer viewCount;

    private List<String> images;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
