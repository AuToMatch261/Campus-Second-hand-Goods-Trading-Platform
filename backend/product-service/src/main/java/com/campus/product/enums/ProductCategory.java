package com.campus.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.campus.common.enums.CodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductCategory implements CodeEnum {

    BOOKS(1, "书籍"),
    ELECTRONICS(2, "电子"),
    DAILY(3, "日用"),
    CLOTHING(4, "服装"),
    SPORTS(5, "运动"),
    OTHER(6, "其它");

    @EnumValue
    @JsonValue
    private final int code;

    private final String label;

    ProductCategory(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ProductCategory of(Integer code) {
        if (code == null) return null;
        for (ProductCategory c : values()) {
            if (c.code == code) return c;
        }
        return null;
    }
}
