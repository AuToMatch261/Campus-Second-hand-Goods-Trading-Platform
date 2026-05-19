package com.campus.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "下单请求")
public class CreateOrderRequest {

    @Schema(description = "商品 ID")
    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    @Schema(description = "买家看到的成交价格，用于校验商品价格是否变动")
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "价格不能为负")
    @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数 2 位小数")
    private BigDecimal price;

    @Schema(description = "给卖家的留言")
    @Size(max = 255, message = "留言最长 255 字符")
    private String remark;
}
