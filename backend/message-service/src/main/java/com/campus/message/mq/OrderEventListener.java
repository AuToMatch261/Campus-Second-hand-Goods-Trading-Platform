package com.campus.message.mq;

import com.campus.common.mq.RabbitMqConstants;
import com.campus.common.mq.event.OrderEvent;
import com.campus.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMqConstants.QUEUE_MESSAGE_ORDER_EVENTS)
    public void onOrderEvent(OrderEvent event) {
        log.info("收到订单事件: type={} orderId={} eventId={}",
                event == null ? null : event.getType(),
                event == null ? null : event.getOrderId(),
                event == null ? null : event.getEventId());
        notificationService.onOrderEvent(event);
    }
}
