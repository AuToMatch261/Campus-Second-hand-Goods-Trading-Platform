package com.campus.product.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import com.campus.product.dto.ProductVO;
import com.campus.product.dto.PublishProductRequest;
import com.campus.product.dto.UpdateProductRequest;
import com.campus.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "my-product", description = "商品私有接口：发布 / 编辑 / 下架 / 我的（需登录）")
@RestController
@RequestMapping("/me/products")
@RequiredArgsConstructor
public class MyProductController {

    private final ProductService service;

    @Operation(summary = "发布商品")
    @PostMapping
    public Result<ProductVO> publish(@CurrentUser Long userId,
                                    @Valid @RequestBody PublishProductRequest req) {
        return Result.ok(service.publish(userId, req));
    }

    @Operation(summary = "我的商品列表")
    @GetMapping
    public Result<PageResult<ProductVO>> mine(@CurrentUser Long userId,
                                             @Parameter(description = "页码，1 起") @RequestParam(defaultValue = "1") long page,
                                             @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") long size) {
        return Result.ok(service.listMine(userId, page, size));
    }

    @Operation(summary = "更新自己的商品（只更新非 null 字段）")
    @PutMapping("/{id}")
    public Result<ProductVO> update(@CurrentUser Long userId,
                                   @PathVariable long id,
                                   @Valid @RequestBody UpdateProductRequest req) {
        return Result.ok(service.update(userId, id, req));
    }

    @Operation(summary = "下架自己的商品")
    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@CurrentUser Long userId, @PathVariable long id) {
        service.offShelf(userId, id);
        return Result.ok();
    }

    @Operation(summary = "删除自己的商品（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@CurrentUser Long userId, @PathVariable long id) {
        service.softDelete(userId, id);
        return Result.ok();
    }
}
