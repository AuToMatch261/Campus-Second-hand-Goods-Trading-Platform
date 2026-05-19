package com.campus.common.security;

import com.campus.common.enums.CodeEnumConverterFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class WebMvcContextConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        // 让 URL query 参数(如 ?category=1)能转成实现 CodeEnum 接口的枚举,
        // Spring 默认只按枚举名匹配,不认 @JsonValue
        registry.addConverterFactory(new CodeEnumConverterFactory());
    }
}
