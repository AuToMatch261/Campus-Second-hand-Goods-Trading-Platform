package com.campus.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.campus.product.config.SearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 应用启动时确保 products 索引存在；如果不存在，按 product-index.json 创建。
 * 已存在则不动（避免覆盖运行时 mapping）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexInitializer implements ApplicationRunner {

    private final ElasticsearchClient esClient;
    private final SearchProperties props;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(ApplicationArguments args) {
        String index = props.getProductsIndex();
        try {
            boolean exists = esClient.indices().exists(b -> b.index(index)).value();
            if (exists) {
                log.info("ES 索引已存在，跳过创建: {}", index);
                return;
            }
            Resource resource = resourceLoader.getResource("classpath:es/product-index.json");
            try (InputStream is = resource.getInputStream()) {
                esClient.indices().create(b -> b.index(index).withJson(is));
            }
            log.info("ES 索引已创建: {}", index);
        } catch (Exception e) {
            log.error("初始化 ES 索引失败: index={} err={}", index, e.getMessage(), e);
            // 索引初始化失败不阻塞服务启动；商品发布/搜索可在 ES 可用后再使用
        }
    }
}
