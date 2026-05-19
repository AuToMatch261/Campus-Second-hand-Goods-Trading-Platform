package com.campus.common.enums;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * String → Enum 转换工厂。Enum 必须实现 {@link CodeEnum}。
 *
 * 解决场景:URL query 参数携带数字 code(如 ?category=1),Spring 默认按枚举名匹配
 * 找不到,会报 "Failed to convert from type [java.lang.String] to type [...Enum]"。
 *
 * 注册位置:{@code WebMvcContextConfig#addFormatters} (common 模块)。
 */
public class CodeEnumConverterFactory implements ConverterFactory<String, CodeEnum> {

    /** 按 enum 类型缓存 code→实例 映射,避免每次请求都遍历 enum constants */
    private static final Map<Class<? extends CodeEnum>, Map<Integer, ? extends CodeEnum>> CACHE =
            new ConcurrentHashMap<>();

    @Override
    @NonNull
    public <T extends CodeEnum> Converter<String, T> getConverter(@NonNull Class<T> targetType) {
        return new StringToCodeEnum<>(targetType);
    }

    private static final class StringToCodeEnum<T extends CodeEnum> implements Converter<String, T> {

        private final Class<T> targetType;

        StringToCodeEnum(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T convert(@NonNull String source) {
            if (source.isBlank()) return null;
            int code;
            try {
                code = Integer.parseInt(source.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "枚举 " + targetType.getSimpleName() + " 仅接受 int code,收到: " + source);
            }
            Map<Integer, T> map = (Map<Integer, T>) CACHE.computeIfAbsent(targetType,
                    type -> buildMap(targetType));
            T value = map.get(code);
            if (value == null) {
                throw new IllegalArgumentException(
                        "枚举 " + targetType.getSimpleName() + " 不存在 code=" + code);
            }
            return value;
        }

        private Map<Integer, T> buildMap(Class<T> type) {
            T[] constants = type.getEnumConstants();
            if (constants == null) {
                throw new IllegalStateException(type + " 不是枚举");
            }
            Map<Integer, T> map = new HashMap<>(constants.length * 2);
            for (T c : constants) {
                map.put(c.getCode(), c);
            }
            return map;
        }
    }
}
