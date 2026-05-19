package com.campus.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Feign 调 product-service 内部接口的反序列化结构；
 * 只声明 order-service 关心的字段，其余被忽略。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSnapshot {

    private Long id;
    private Long sellerId;
    private String title;
    private BigDecimal price;
    /** 1=ON_SHELF 2=SOLD 3=OFF_SHELF */
    private Integer status;
    private List<String> images;
}
