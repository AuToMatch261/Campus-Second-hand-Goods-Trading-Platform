package com.campus.order.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import com.campus.order.dto.CreateOrderRequest;
import com.campus.order.dto.OrderVO;
import com.campus.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "order", description = "订单：下单 / 确认 / 取消 / 列表")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @Operation(summary = "下单")
    @PostMapping
    public Result<OrderVO> create(@CurrentUser Long userId,
                                 @Valid @RequestBody CreateOrderRequest req) {
        return Result.ok(service.create(userId, req));
    }

    @Operation(summary = "买家确认完成")
    @PostMapping("/{id}/confirm")
    public Result<OrderVO> confirm(@CurrentUser Long userId, @PathVariable long id) {
        return Result.ok(service.confirm(userId, id));
    }

    @Operation(summary = "取消订单（买卖双方均可）")
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@CurrentUser Long userId, @PathVariable long id) {
        return Result.ok(service.cancel(userId, id));
    }

    @Operation(summary = "订单详情（仅订单双方可见）")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@CurrentUser Long userId, @PathVariable long id) {
        return Result.ok(service.detail(userId, id));
    }

    @Operation(summary = "我购买的订单")
    @GetMapping("/buyer/mine")
    public Result<PageResult<OrderVO>> mineAsBuyer(@CurrentUser Long userId,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size) {
        return Result.ok(service.listAsBuyer(userId, page, size));
    }

    @Operation(summary = "我收到的订单")
    @GetMapping("/seller/mine")
    public Result<PageResult<OrderVO>> mineAsSeller(@CurrentUser Long userId,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size) {
        return Result.ok(service.listAsSeller(userId, page, size));
    }
}
