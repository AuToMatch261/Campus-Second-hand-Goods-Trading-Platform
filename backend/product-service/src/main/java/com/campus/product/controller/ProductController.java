package com.campus.product.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.Result;
import com.campus.product.dto.ProductListQuery;
import com.campus.product.dto.ProductVO;
import com.campus.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "product", description = "商品公开接口：列表 / 详情（游客可访问）")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @Operation(summary = "商品详情（自动 +1 浏览量）")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable long id) {
        return Result.ok(service.detail(id, true));
    }

    @Operation(summary = "商品列表（默认只看在售）")
    @GetMapping
    public Result<PageResult<ProductVO>> list(@Valid ProductListQuery query) {
        return Result.ok(service.list(query));
    }
}
