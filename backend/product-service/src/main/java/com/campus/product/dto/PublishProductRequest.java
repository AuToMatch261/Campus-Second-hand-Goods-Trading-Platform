package com.campus.product.dto;

import com.campus.product.enums.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "发布商品请求")
public class PublishProductRequest {

    @Schema(description = "标题", example = "九成新数据库系统概念")
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字符")
    private String title;

    @Schema(description = "描述", example = "买来没怎么翻过，扉页有签名")
    @Size(max = 2000, message = "描述最长 2000 字符")
    private String description;

    @Schema(description = "价格", example = "29.90")
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "价格不能为负")
    @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数 2 位小数")
    private BigDecimal price;

    @Schema(description = "分类（1=书籍 2=电子 3=日用 4=服装 5=运动 6=其它）")
    @NotNull(message = "分类不能为空")
    private ProductCategory category;

    @Schema(description = "图片 URL 列表")
    @Size(max = 9, message = "图片最多 9 张")
    private List<@NotBlank @Size(max = 255) String> images;
}
