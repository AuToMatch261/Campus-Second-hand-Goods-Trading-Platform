package com.campus.order.mq;

import com.campus.common.mq.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 监听本地 Spring 事件，事务提交后再投递到 MQ。
 * 如果事务回滚，事件不会发出，避免出现"DB 没写成但消息已发"的情况。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventForwarder {

    private final OrderEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        try {
            publisher.publish(event);
        } catch (Exception e) {
            log.error("订单事件投递 MQ 失败: type={} orderId={} eventId={} err={}",
                    event.getType(), event.getOrderId(), event.getEventId(), e.getMessage(), e);
        }
    }
}
