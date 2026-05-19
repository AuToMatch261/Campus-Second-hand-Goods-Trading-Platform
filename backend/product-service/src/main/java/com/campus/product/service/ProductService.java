package com.campus.product.service;

import com.campus.common.response.PageResult;
import com.campus.product.dto.ProductListQuery;
import com.campus.product.dto.ProductVO;
import com.campus.product.dto.PublishProductRequest;
import com.campus.product.dto.UpdateProductRequest;

public interface ProductService {

    ProductVO publish(long sellerId, PublishProductRequest req);

    ProductVO update(long sellerId, long productId, UpdateProductRequest req);

    void offShelf(long sellerId, long productId);

    void softDelete(long sellerId, long productId);

    ProductVO detail(long productId, boolean increaseView);

    PageResult<ProductVO> list(ProductListQuery query);

    PageResult<ProductVO> listMine(long sellerId, long page, long size);

    /** 内部：CAS 将商品从 ON_SHELF 改为 SOLD；失败抛 PRODUCT_OFF_SHELF */
    void markSold(long productId);

    /**
     * 幂等版恢复上架（事件消费端用）：
     * - 已是 ON_SHELF：no-op
     * - SOLD → ON_SHELF
     * - 其他状态或商品不存在：返回 false（让调用方决定是否要重试 / 告警）
     */
    boolean tryRelist(long productId);
}
