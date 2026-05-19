package com.campus.product.dto;

import com.campus.product.enums.ProductCategory;
import com.campus.product.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "商品列表查询参数")
public class ProductListQuery {

    @Schema(description = "页码（1 起）", defaultValue = "1")
    @Min(value = 1, message = "page 至少为 1")
    private long page = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    @Min(value = 1, message = "size 至少为 1")
    @Max(value = 100, message = "size 最多 100")
    private long size = 20;

    @Schema(description = "分类过滤；为空返回全部")
    private ProductCategory category;

    @Schema(description = "状态过滤；为空时默认返回在售")
    private ProductStatus status;

    @Schema(description = "关键字（在标题 + 描述上做中文分词检索）")
    private String keyword;

    @Schema(description = "限定卖家用户 ID")
    private Long sellerId;

    @Schema(description = "排序字段：createdAt(默认) / price / viewCount", defaultValue = "createdAt")
    private String sortBy = "createdAt";

    @Schema(description = "排序方向：desc(默认) / asc", defaultValue = "desc")
    private String sortOrder = "desc";
}
