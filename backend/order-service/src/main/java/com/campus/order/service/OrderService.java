package com.campus.order.service;

import com.campus.common.response.PageResult;
import com.campus.order.dto.CreateOrderRequest;
import com.campus.order.dto.OrderVO;

public interface OrderService {

    OrderVO create(long buyerId, CreateOrderRequest req);

    OrderVO confirm(long buyerId, long orderId);

    OrderVO cancel(long userId, long orderId);

    OrderVO detail(long userId, long orderId);

    PageResult<OrderVO> listAsBuyer(long buyerId, long page, long size);

    PageResult<OrderVO> listAsSeller(long sellerId, long page, long size);
}
