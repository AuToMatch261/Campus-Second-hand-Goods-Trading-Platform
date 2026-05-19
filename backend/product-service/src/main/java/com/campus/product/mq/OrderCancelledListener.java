package com.campus.product.mq;

import com.campus.common.mq.RabbitMqConstants;
import com.campus.common.mq.event.OrderEvent;
import com.campus.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledListener {

    private final ProductService productService;

    @RabbitListener(queues = RabbitMqConstants.QUEUE_PRODUCT_ORDER_CANCELLED)
    public void onOrderCancelled(OrderEvent event) {
        if (event == null || event.getProductId() == null) {
            log.warn("收到非法订单取消事件: {}", event);
            return;
        }
        log.info("收到订单取消事件，开始恢复商品上架: orderId={} productId={}",
                event.getOrderId(), event.getProductId());
        productService.tryRelist(event.getProductId());
    }
}
