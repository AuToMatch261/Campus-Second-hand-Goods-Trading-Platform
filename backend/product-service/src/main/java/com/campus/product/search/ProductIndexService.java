package com.campus.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.campus.product.config.SearchProperties;
import com.campus.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 负责把商品写入 / 从 ES 删除。所有方法捕获并打印异常，避免 ES 故障扩散到主业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final ElasticsearchClient esClient;
    private final SearchProperties props;

    public void upsert(Product p) {
        if (p == null) return;
        ProductDoc doc = toDoc(p);
        try {
            esClient.index(b -> b
                    .index(props.getProductsIndex())
                    .id(String.valueOf(doc.getId()))
                    .document(doc));
        } catch (IOException | RuntimeException e) {
            log.error("ES 写入失败: productId={} err={}", doc.getId(), e.getMessage(), e);
            throw new IndexingException("ES 写入失败: " + e.getMessage(), e);
        }
    }

    public void delete(long productId) {
        try {
            esClient.delete(b -> b
                    .index(props.getProductsIndex())
                    .id(String.valueOf(productId)));
        } catch (IOException | RuntimeException e) {
            log.error("ES 删除失败: productId={} err={}", productId, e.getMessage(), e);
            throw new IndexingException("ES 删除失败: " + e.getMessage(), e);
        }
    }

    /** 批量 upsert，返回成功数；reindex 用。 */
    public int bulkUpsert(List<Product> products) {
        if (products == null || products.isEmpty()) return 0;
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Product p : products) {
            ProductDoc doc = toDoc(p);
            br.operations(BulkOperation.of(op -> op.index(idx -> idx
                    .index(props.getProductsIndex())
                    .id(String.valueOf(doc.getId()))
                    .document(doc))));
        }
        try {
            BulkResponse resp = esClient.bulk(br.build());
            if (resp.errors()) {
                long failures = resp.items().stream().filter(i -> i.error() != null).count();
                log.warn("bulkUpsert 部分失败：total={} failures={}", resp.items().size(), failures);
                return resp.items().size() - (int) failures;
            }
            return resp.items().size();
        } catch (IOException | RuntimeException e) {
            log.error("ES bulkUpsert 失败: size={} err={}", products.size(), e.getMessage(), e);
            throw new IndexingException("ES bulkUpsert 失败: " + e.getMessage(), e);
        }
    }

    private static ProductDoc toDoc(Product p) {
        return ProductDoc.builder()
                .id(p.getId())
                .sellerId(p.getSellerId())
                .title(p.getTitle())
                .description(p.getDescription())
                .category(p.getCategory() == null ? null : p.getCategory().getCode())
                .status(p.getStatus() == null ? null : p.getStatus().getCode())
                .price(p.getPrice())
                .viewCount(p.getViewCount() == null ? 0 : p.getViewCount())
                .images(p.getImages())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public static class IndexingException extends RuntimeException {
        public IndexingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
