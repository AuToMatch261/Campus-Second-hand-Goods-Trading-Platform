package com.campus.order.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.campus.common.enums.CodeEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OrderStatus implements CodeEnum {

    PENDING(1, "待完成"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    @EnumValue
    @JsonValue
    private final int code;

    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
