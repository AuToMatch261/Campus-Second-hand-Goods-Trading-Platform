package com.campus.product.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.campus.common.response.PageResult;
import com.campus.product.config.SearchProperties;
import com.campus.product.dto.ProductListQuery;
import com.campus.product.dto.ProductVO;
import com.campus.product.enums.ProductCategory;
import com.campus.product.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient esClient;
    private final SearchProperties props;

    public PageResult<ProductVO> search(ProductListQuery q) {
        String index = props.getProductsIndex();
        int from = (int) Math.max(0, (q.getPage() - 1) * q.getSize());
        int size = (int) Math.min(100, Math.max(1, q.getSize()));

        BoolQuery.Builder bool = new BoolQuery.Builder();

        ProductStatus status = q.getStatus() == null ? ProductStatus.ON_SHELF : q.getStatus();
        bool.filter(Query.of(qb -> qb.term(t -> t.field("status").value(status.getCode()))));

        if (q.getCategory() != null) {
            bool.filter(Query.of(qb -> qb.term(t -> t.field("category").value(q.getCategory().getCode()))));
        }
        if (q.getSellerId() != null) {
            bool.filter(Query.of(qb -> qb.term(t -> t.field("sellerId").value(q.getSellerId()))));
        }
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String kw = q.getKeyword().trim();
            bool.must(Query.of(qb -> qb.multiMatch(m -> m
                    .fields("title^3", "description")
                    .query(kw)
                    // IK 分词 + ik_smart 搜索；分词后任意命中即算
                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or))));
        }

        String sortField = resolveSortField(q.getSortBy());
        SortOrder sortOrder = "asc".equalsIgnoreCase(q.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;

        Query finalQuery = Query.of(qb -> qb.bool(bool.build()));

        try {
            SearchResponse<ProductDoc> resp = esClient.search(s -> s
                            .index(index)
                            .from(from)
                            .size(size)
                            .query(finalQuery)
                            .sort(srt -> srt.field(f -> f.field(sortField).order(sortOrder))),
                    ProductDoc.class);

            long total = resp.hits().total() == null ? 0 : resp.hits().total().value();
            List<ProductVO> records = resp.hits().hits().stream()
                    .map(Hit::source)
                    .filter(java.util.Objects::nonNull)
                    .map(ProductSearchService::toVO)
                    .toList();

            return PageResult.of(q.getPage(), q.getSize(), total, records);
        } catch (IOException | RuntimeException e) {
            log.error("ES 搜索失败: {}", e.getMessage(), e);
            // 返回空结果 + 0 total，让前端展示空态而不是 500
            return PageResult.of(q.getPage(), q.getSize(), 0, List.of());
        }
    }

    private static String resolveSortField(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy) {
            case "price" -> "price";
            case "viewCount" -> "viewCount";
            default -> "createdAt";
        };
    }

    private static ProductVO toVO(ProductDoc d) {
        ProductCategory category = ProductCategory.of(d.getCategory());
        ProductStatus status = d.getStatus() == null ? null
                : switch (d.getStatus()) {
            case 1 -> ProductStatus.ON_SHELF;
            case 2 -> ProductStatus.SOLD;
            case 3 -> ProductStatus.OFF_SHELF;
            default -> null;
        };
        return ProductVO.builder()
                .id(d.getId())
                .sellerId(d.getSellerId())
                .title(d.getTitle())
                .description(d.getDescription())
                .price(d.getPrice())
                .category(category)
                .categoryLabel(category == null ? null : category.getLabel())
                .images(d.getImages() == null ? List.of() : d.getImages())
                .status(status)
                .statusLabel(status == null ? null : status.getLabel())
                .viewCount(d.getViewCount() == null ? 0 : d.getViewCount())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
