package com.campus.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.exception.BusinessException;
import com.campus.common.mq.event.ProductEvent;
import com.campus.common.response.PageResult;
import com.campus.common.response.ResultCode;
import com.campus.product.dto.ProductListQuery;
import com.campus.product.dto.ProductVO;
import com.campus.product.dto.PublishProductRequest;
import com.campus.product.dto.UpdateProductRequest;
import com.campus.product.entity.Product;
import com.campus.product.enums.ProductStatus;
import com.campus.product.mapper.ProductMapper;
import com.campus.product.search.ProductSearchService;
import com.campus.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ApplicationEventPublisher events;
    private final ProductSearchService searchService;

    @Override
    @Transactional
    public ProductVO publish(long sellerId, PublishProductRequest req) {
        Product p = new Product();
        p.setSellerId(sellerId);
        p.setTitle(req.getTitle());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setCategory(req.getCategory());
        p.setImages(req.getImages());
        p.setStatus(ProductStatus.ON_SHELF);
        p.setViewCount(0);
        productMapper.insert(p);
        log.info("商品上架: id={} seller={} title={}", p.getId(), sellerId, p.getTitle());
        publishUpserted(p.getId());
        return toVO(p);
    }

    @Override
    @Transactional
    public ProductVO update(long sellerId, long productId, UpdateProductRequest req) {
        Product existing = mustExist(productId);
        ensureOwner(existing, sellerId);
        if (existing.getStatus() == ProductStatus.SOLD) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "已售商品不可修改");
        }

        Product patch = new Product();
        patch.setId(productId);
        if (req.getTitle()       != null) patch.setTitle(req.getTitle());
        if (req.getDescription() != null) patch.setDescription(req.getDescription());
        if (req.getPrice()       != null) patch.setPrice(req.getPrice());
        if (req.getCategory()    != null) patch.setCategory(req.getCategory());
        if (req.getImages()      != null) patch.setImages(req.getImages());
        productMapper.updateById(patch);

        publishUpserted(productId);
        return toVO(productMapper.selectById(productId));
    }

    @Override
    @Transactional
    public void offShelf(long sellerId, long productId) {
        Product existing = mustExist(productId);
        ensureOwner(existing, sellerId);
        if (existing.getStatus() != ProductStatus.ON_SHELF) {
            throw new BusinessException(ResultCode.PRODUCT_STATUS_ILLEGAL, "仅在售商品可下架");
        }
        Product patch = new Product();
        patch.setId(productId);
        patch.setStatus(ProductStatus.OFF_SHELF);
        productMapper.updateById(patch);
        publishUpserted(productId);
    }

    @Override
    @Transactional
    public void softDelete(long sellerId, long productId) {
        Product existing = mustExist(productId);
        ensureOwner(existing, sellerId);
        productMapper.deleteById(productId);
        publishDeleted(productId);
    }

    @Override
    @Transactional
    public ProductVO detail(long productId, boolean increaseView) {
        Product p = mustExist(productId);
        if (increaseView && p.getStatus() == ProductStatus.ON_SHELF) {
            productMapper.incrementViewCount(productId);
            p.setViewCount((p.getViewCount() == null ? 0 : p.getViewCount()) + 1);
            // viewCount 高频写，不发事件；ES 的 viewCount 由 reindex 兜底
        }
        return toVO(p);
    }

    @Override
    public PageResult<ProductVO> list(ProductListQuery q) {
        return searchService.search(q);
    }

    @Override
    public PageResult<ProductVO> listMine(long sellerId, long page, long size) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .orderByDesc(Product::getCreatedAt);
        Page<Product> p = productMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(ProductServiceImpl::toVO).toList());
    }

    @Override
    @Transactional
    public void markSold(long productId) {
        Product p = mustExist(productId);
        int n = productMapper.updateStatusConditional(productId,
                ProductStatus.ON_SHELF.getCode(), ProductStatus.SOLD.getCode());
        if (n == 0) {
            if (p.getStatus() == ProductStatus.SOLD) {
                throw new BusinessException(ResultCode.PRODUCT_SOLD);
            }
            throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF);
        }
        log.info("商品锁定为已售: id={}", productId);
        publishUpserted(productId);
    }

    @Override
    @Transactional
    public boolean tryRelist(long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) {
            log.warn("订单取消事件: 商品不存在，跳过恢复上架 productId={}", productId);
            return false;
        }
        if (p.getStatus() == ProductStatus.ON_SHELF) {
            return true;
        }
        if (p.getStatus() != ProductStatus.SOLD) {
            log.warn("订单取消事件: 商品状态非 SOLD/ON_SHELF，跳过恢复上架 productId={} status={}",
                    productId, p.getStatus());
            return false;
        }
        int n = productMapper.updateStatusConditional(productId,
                ProductStatus.SOLD.getCode(), ProductStatus.ON_SHELF.getCode());
        if (n == 0) {
            // 并发：选中时是 SOLD，CAS 时已被改走 → 再读一次决定结果
            Product latest = productMapper.selectById(productId);
            return latest != null && latest.getStatus() == ProductStatus.ON_SHELF;
        }
        log.info("商品恢复上架: id={}", productId);
        publishUpserted(productId);
        return true;
    }

    private void publishUpserted(long productId) {
        events.publishEvent(ProductEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(ProductEvent.Type.UPSERTED)
                .productId(productId)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void publishDeleted(long productId) {
        events.publishEvent(ProductEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(ProductEvent.Type.DELETED)
                .productId(productId)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private Product mustExist(long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return p;
    }

    private void ensureOwner(Product p, long sellerId) {
        if (p.getSellerId() == null || p.getSellerId() != sellerId) {
            throw new BusinessException(ResultCode.PRODUCT_OWNER_MISMATCH);
        }
    }

    private static ProductVO toVO(Product p) {
        return ProductVO.builder()
                .id(p.getId())
                .sellerId(p.getSellerId())
                .title(p.getTitle())
                .description(p.getDescription())
                .price(p.getPrice())
                .category(p.getCategory())
                .categoryLabel(p.getCategory() == null ? null : p.getCategory().getLabel())
                .images(p.getImages() == null ? List.of() : p.getImages())
                .status(p.getStatus())
                .statusLabel(p.getStatus() == null ? null : p.getStatus().getLabel())
                .viewCount(p.getViewCount())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
