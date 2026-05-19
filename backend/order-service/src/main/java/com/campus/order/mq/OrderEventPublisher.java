package com.campus.order.mq;

import com.campus.common.mq.RabbitMqConstants;
import com.campus.common.mq.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderEvent event) {
        String rk = switch (event.getType()) {
            case CREATED -> RabbitMqConstants.RK_ORDER_CREATED;
            case CONFIRMED -> RabbitMqConstants.RK_ORDER_CONFIRMED;
            case CANCELLED -> RabbitMqConstants.RK_ORDER_CANCELLED;
        };
        rabbitTemplate.convertAndSend(RabbitMqConstants.ORDER_EVENTS_EXCHANGE, rk, event);
        log.info("发送订单事件: type={} orderId={} productId={} rk={} eventId={}",
                event.getType(), event.getOrderId(), event.getProductId(), rk, event.getEventId());
    }
}
