package com.campus.product.dto;

import com.campus.product.enums.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "更新商品请求；只更新非 null 字段")
public class UpdateProductRequest {

    @Size(max = 100, message = "标题最长 100 字符")
    private String title;

    @Size(max = 2000, message = "描述最长 2000 字符")
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "价格不能为负")
    @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数 2 位小数")
    private BigDecimal price;

    private ProductCategory category;

    @Size(max = 9, message = "图片最多 9 张")
    private List<@Size(max = 255) String> images;
}
