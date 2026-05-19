package com.campus.common.enums;

/**
 * 带 int code 的枚举公共接口。
 *
 * 业务枚举(如 ProductCategory / OrderStatus)用 {@code code} 作为外部 ID,
 * 实现本接口后即可被 {@link CodeEnumConverterFactory} 用于 URL 查询参数
 * 的字符串 → 枚举转换。
 *
 * 注意 Jackson 反序列化(JSON body)走的是 {@code @JsonValue},
 * 与本接口无关,但通常两者会指向同一个 {@code code} 字段。
 */
public interface CodeEnum {

    int getCode();
}
