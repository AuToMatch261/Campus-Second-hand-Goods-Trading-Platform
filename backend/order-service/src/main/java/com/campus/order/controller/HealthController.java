package com.campus.order.controller;

import com.campus.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "order-health", description = "订单服务健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Operation(summary = "ping")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("order-service pong");
    }
}
