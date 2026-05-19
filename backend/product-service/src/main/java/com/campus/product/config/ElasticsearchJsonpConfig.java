package com.campus.product.config;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 覆盖 Spring Boot 默认的 ES JsonpMapper(它的 ObjectMapper 不带 jsr310 模块,
 * 序列化 {@code LocalDateTime} 会抛 InvalidDefinitionException)。
 *
 * Spring Boot 3.x 的 ElasticsearchClientConfigurations 用 {@code @ConditionalOnMissingBean}
 * 注册 JsonpMapper,所以本 Bean 会被优先使用。
 */
@Configuration
public class ElasticsearchJsonpConfig {

    @Bean
    public JsonpMapper jsonpMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return new JacksonJsonpMapper(mapper);
    }
}
