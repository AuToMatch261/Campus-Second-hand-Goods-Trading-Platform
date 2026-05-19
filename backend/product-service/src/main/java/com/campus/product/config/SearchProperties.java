package com.campus.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "campus.search")
public class SearchProperties {

    private String productsIndex = "products";
}
