package com.campus.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.campus.common.enums.CodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductStatus implements CodeEnum {

    ON_SHELF(1, "在售"),
    SOLD(2, "已售"),
    OFF_SHELF(3, "已下架");

    @EnumValue
    @JsonValue
    private final int code;

    private final String label;

    ProductStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
