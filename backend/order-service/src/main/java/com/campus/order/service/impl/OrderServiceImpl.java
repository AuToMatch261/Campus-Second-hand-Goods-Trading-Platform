package com.campus.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.exception.BusinessException;
import com.campus.common.mq.event.OrderEvent;
import com.campus.common.response.PageResult;
import com.campus.common.response.Result;
import com.campus.common.response.ResultCode;
import com.campus.order.dto.CreateOrderRequest;
import com.campus.order.dto.OrderVO;
import com.campus.order.dto.ProductSnapshot;
import com.campus.order.entity.Order;
import com.campus.order.enums.OrderStatus;
import com.campus.order.feign.ProductInternalClient;
import com.campus.order.mapper.OrderMapper;
import com.campus.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductInternalClient productClient;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public OrderVO create(long buyerId, CreateOrderRequest req) {
        Result<ProductSnapshot> r = productClient.get(req.getProductId());
        if (r == null || r.getCode() != 0 || r.getData() == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        ProductSnapshot p = r.getData();

        if (p.getSellerId() != null && p.getSellerId() == buyerId) {
            throw new BusinessException(ResultCode.ORDER_BUY_OWN_PRODUCT);
        }
        if (p.getStatus() == null || p.getStatus() != 1) {
            throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF, "商品不可购买");
        }
        if (req.getPrice() == null || p.getPrice() == null
                || req.getPrice().compareTo(p.getPrice()) != 0) {
            throw new BusinessException(ResultCode.ORDER_PRODUCT_PRICE_CHANGED);
        }

        // 调用 product-service 把商品状态置为 SOLD（CAS，竞争失败由 product-service 抛业务异常）
        // 这里保留同步调用：防超售必须同步拦截
        Result<Void> sold = productClient.markSold(p.getId());
        if (sold == null || sold.getCode() != 0) {
            int code = sold == null ? ResultCode.SERVER_ERROR.getCode() : sold.getCode();
            String msg = sold == null ? "锁定商品失败" : sold.getMessage();
            throw new BusinessException(code, msg);
        }

        Order order = new Order();
        order.setProductId(p.getId());
        order.setProductTitle(p.getTitle());
        order.setProductImage(p.getImages() == null || p.getImages().isEmpty() ? null : p.getImages().get(0));
        order.setPrice(p.getPrice());
        order.setBuyerId(buyerId);
        order.setSellerId(p.getSellerId());
        order.setStatus(OrderStatus.PENDING);
        order.setRemark(req.getRemark());
        orderMapper.insert(order);
        log.info("订单已创建: id={} product={} buyer={} seller={}",
                order.getId(), p.getId(), buyerId, p.getSellerId());

        events.publishEvent(OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(OrderEvent.Type.CREATED)
                .orderId(order.getId())
                .productId(order.getProductId())
                .productTitle(order.getProductTitle())
                .price(order.getPrice())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .orderStatus(OrderStatus.PENDING.name())
                .occurredAt(LocalDateTime.now())
                .build());

        return toVO(order);
    }

    @Override
    @Transactional
    public OrderVO confirm(long buyerId, long orderId) {
        Order order = mustExist(orderId);
        if (order.getBuyerId() != buyerId) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED, "只有买家可以确认完成");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL, "订单非待完成，不能确认");
        }

        Order patch = new Order();
        patch.setId(orderId);
        patch.setStatus(OrderStatus.COMPLETED);
        patch.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(patch);

        Order updated = orderMapper.selectById(orderId);

        events.publishEvent(OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(OrderEvent.Type.CONFIRMED)
                .orderId(updated.getId())
                .productId(updated.getProductId())
                .productTitle(updated.getProductTitle())
                .price(updated.getPrice())
                .buyerId(updated.getBuyerId())
                .sellerId(updated.getSellerId())
                .orderStatus(OrderStatus.COMPLETED.name())
                .occurredAt(LocalDateTime.now())
                .build());

        return toVO(updated);
    }

    @Override
    @Transactional
    public OrderVO cancel(long userId, long orderId) {
        Order order = mustExist(orderId);
        if (order.getBuyerId() != userId && order.getSellerId() != userId) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL, "订单非待完成，不能取消");
        }

        Order patch = new Order();
        patch.setId(orderId);
        patch.setStatus(OrderStatus.CANCELLED);
        patch.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(patch);

        Order updated = orderMapper.selectById(orderId);
        String cancelledBy = userId == updated.getBuyerId() ? "BUYER" : "SELLER";

        // 不再同步调 productClient.relist：交给 product-service 消费 order.cancelled 事件
        events.publishEvent(OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(OrderEvent.Type.CANCELLED)
                .orderId(updated.getId())
                .productId(updated.getProductId())
                .productTitle(updated.getProductTitle())
                .price(updated.getPrice())
                .buyerId(updated.getBuyerId())
                .sellerId(updated.getSellerId())
                .orderStatus(OrderStatus.CANCELLED.name())
                .cancelledBy(cancelledBy)
                .occurredAt(LocalDateTime.now())
                .build());

        return toVO(updated);
    }

    @Override
    public OrderVO detail(long userId, long orderId) {
        Order order = mustExist(orderId);
        if (order.getBuyerId() != userId && order.getSellerId() != userId) {
            throw new BusinessException(ResultCode.ORDER_ACCESS_DENIED);
        }
        return toVO(order);
    }

    @Override
    public PageResult<OrderVO> listAsBuyer(long buyerId, long page, long size) {
        return paged(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .orderByDesc(Order::getCreatedAt), page, size);
    }

    @Override
    public PageResult<OrderVO> listAsSeller(long sellerId, long page, long size) {
        return paged(new LambdaQueryWrapper<Order>()
                .eq(Order::getSellerId, sellerId)
                .orderByDesc(Order::getCreatedAt), page, size);
    }

    private PageResult<OrderVO> paged(LambdaQueryWrapper<Order> w, long page, long size) {
        Page<Order> p = orderMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(OrderServiceImpl::toVO).toList());
    }

    private Order mustExist(long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        return o;
    }

    private static OrderVO toVO(Order o) {
        return OrderVO.builder()
                .id(o.getId())
                .productId(o.getProductId())
                .productTitle(o.getProductTitle())
                .productImage(o.getProductImage())
                .price(o.getPrice())
                .buyerId(o.getBuyerId())
                .sellerId(o.getSellerId())
                .status(o.getStatus())
                .statusLabel(o.getStatus() == null ? null : o.getStatus().getLabel())
                .remark(o.getRemark())
                .createdAt(o.getCreatedAt())
                .completedAt(o.getCompletedAt())
                .cancelledAt(o.getCancelledAt())
                .build();
    }
}
