package com.campus.product.dto;

import com.campus.product.enums.ProductCategory;
import com.campus.product.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "商品信息")
public class ProductVO {

    private Long id;

    private Long sellerId;

    private String title;

    private String description;

    private BigDecimal price;

    private ProductCategory category;

    @Schema(description = "分类中文名")
    private String categoryLabel;

    private List<String> images;

    private ProductStatus status;

    @Schema(description = "状态中文名")
    private String statusLabel;

    private Integer viewCount;

    private LocalDateTime createdAt;
}
