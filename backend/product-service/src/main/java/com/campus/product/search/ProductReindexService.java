package com.campus.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.product.config.SearchProperties;
import com.campus.product.entity.Product;
import com.campus.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReindexService {

    private static final int BATCH_SIZE = 500;

    private final ProductMapper productMapper;
    private final ProductIndexService indexService;
    private final ElasticsearchClient esClient;
    private final SearchProperties props;
    private final ResourceLoader resourceLoader;

    /**
     * 把 t_product 全量重建到 ES。
     * purge=true 会先删掉索引再按 product-index.json 重建（避免旧字段残留 / 解决 mapping 漂移）。
     * 返回成功写入的文档数。
     */
    public int reindexAll(boolean purge) {
        String index = props.getProductsIndex();

        if (purge) {
            log.info("重建索引：先删除 {}", index);
            try {
                esClient.indices().delete(b -> b.index(index));
            } catch (Exception e) {
                log.warn("删除索引失败（可能本来就不存在）: {}", e.getMessage());
            }
            try {
                Resource resource = resourceLoader.getResource("classpath:es/product-index.json");
                try (InputStream is = resource.getInputStream()) {
                    esClient.indices().create(b -> b.index(index).withJson(is));
                }
                log.info("索引已重建: {}", index);
            } catch (Exception e) {
                throw new IllegalStateException("重建索引失败: " + e.getMessage(), e);
            }
        }

        int total = 0;
        long pageNo = 1;
        while (true) {
            Page<Product> page = productMapper.selectPage(
                    new Page<>(pageNo, BATCH_SIZE),
                    new LambdaQueryWrapper<Product>().orderByAsc(Product::getId));
            if (page.getRecords().isEmpty()) break;

            int ok = indexService.bulkUpsert(page.getRecords());
            total += ok;
            log.info("reindex 进度: page={} batch={} accumulated={}",
                    pageNo, page.getRecords().size(), total);

            if (page.getRecords().size() < BATCH_SIZE) break;
            pageNo++;
        }
        log.info("reindex 完成: total={}", total);
        return total;
    }
}
