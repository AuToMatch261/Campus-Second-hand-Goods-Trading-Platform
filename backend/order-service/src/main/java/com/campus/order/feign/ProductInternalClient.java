package com.campus.order.feign;

import com.campus.common.response.Result;
import com.campus.order.dto.ProductSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "product-service", path = "/internal/product")
public interface ProductInternalClient {

    @GetMapping("/{id}")
    Result<ProductSnapshot> get(@PathVariable("id") long id);

    @PostMapping("/{id}/sold")
    Result<Void> markSold(@PathVariable("id") long id);
}
