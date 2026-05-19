package com.campus.product.mq;

import com.campus.common.mq.RabbitMqConstants;
import com.campus.common.mq.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(ProductEvent event) {
        String rk = switch (event.getType()) {
            case UPSERTED -> RabbitMqConstants.RK_PRODUCT_UPSERTED;
            case DELETED -> RabbitMqConstants.RK_PRODUCT_DELETED;
        };
        rabbitTemplate.convertAndSend(RabbitMqConstants.PRODUCT_EVENTS_EXCHANGE, rk, event);
        log.info("发送商品事件: type={} productId={} rk={} eventId={}",
                event.getType(), event.getProductId(), rk, event.getEventId());
    }
}
