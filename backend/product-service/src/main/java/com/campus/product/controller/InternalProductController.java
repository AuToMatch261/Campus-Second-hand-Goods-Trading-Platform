package com.campus.product.controller;

import com.campus.common.response.Result;
import com.campus.product.dto.ProductVO;
import com.campus.product.search.ProductReindexService;
import com.campus.product.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅供内部 Feign / 运维操作：网关已配置拒绝 /internal/** 的外部访问。
 */
@Hidden
@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService service;
    private final ProductReindexService reindexService;

    @GetMapping("/{id}")
    public Result<ProductVO> get(@PathVariable long id) {
        return Result.ok(service.detail(id, false));
    }

    @PostMapping("/{id}/sold")
    public Result<Void> markSold(@PathVariable long id) {
        service.markSold(id);
        return Result.ok();
    }

    /**
     * 重建 ES 索引：把 t_product 全量灌入 products 索引。
     * purge=true 时先删索引再按 product-index.json 重建（解决 mapping 漂移）。
     */
    @PostMapping("/reindex")
    public Result<Integer> reindex(@RequestParam(defaultValue = "false") boolean purge) {
        return Result.ok(reindexService.reindexAll(purge));
    }
}
