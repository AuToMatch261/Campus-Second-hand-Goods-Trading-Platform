package com.campus.product.mq;

import com.campus.common.mq.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventForwarder {

    private final ProductEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductEvent(ProductEvent event) {
        try {
            publisher.publish(event);
        } catch (Exception e) {
            log.error("商品事件投递 MQ 失败: type={} productId={} eventId={} err={}",
                    event.getType(), event.getProductId(), event.getEventId(), e.getMessage(), e);
        }
    }
}
