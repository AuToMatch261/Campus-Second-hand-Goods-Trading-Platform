package com.campus.message.service;

import com.campus.common.mq.event.OrderEvent;
import com.campus.message.entity.Notification;
import com.campus.message.mapper.NotificationMapper;
import com.campus.message.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    NotificationMapper mapper;

    NotificationServiceImpl service;

    @BeforeEach
    void setup() {
        service = new NotificationServiceImpl(mapper);
    }

    private OrderEvent base(OrderEvent.Type type) {
        return OrderEvent.builder()
                .eventId("evt-1")
                .type(type)
                .orderId(100L)
                .productId(200L)
                .productTitle("旧书")
                .price(new BigDecimal("29.90"))
                .buyerId(7L)
                .sellerId(99L)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Test
    void created_notifies_seller() {
        service.onOrderEvent(base(OrderEvent.Type.CREATED));

        ArgumentCaptor<Notification> c = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(c.capture());
        Notification n = c.getValue();
        assertThat(n.getUserId()).isEqualTo(99L);
        assertThat(n.getType()).isEqualTo("ORDER_CREATED");
        assertThat(n.getRefId()).isEqualTo(100L);
        assertThat(n.getEventId()).isEqualTo("evt-1");
    }

    @Test
    void confirmed_notifies_seller() {
        service.onOrderEvent(base(OrderEvent.Type.CONFIRMED));

        ArgumentCaptor<Notification> c = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(c.capture());
        assertThat(c.getValue().getUserId()).isEqualTo(99L);
        assertThat(c.getValue().getType()).isEqualTo("ORDER_CONFIRMED");
    }

    @Test
    void cancelled_by_buyer_notifies_seller() {
        OrderEvent e = base(OrderEvent.Type.CANCELLED);
        e.setCancelledBy("BUYER");
        service.onOrderEvent(e);

        ArgumentCaptor<Notification> c = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(c.capture());
        assertThat(c.getValue().getUserId()).isEqualTo(99L);
        assertThat(c.getValue().getType()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void cancelled_by_seller_notifies_buyer() {
        OrderEvent e = base(OrderEvent.Type.CANCELLED);
        e.setCancelledBy("SELLER");
        service.onOrderEvent(e);

        ArgumentCaptor<Notification> c = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(c.capture());
        assertThat(c.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    void duplicate_event_is_swallowed() {
        when(mapper.insert(any(Notification.class)))
                .thenThrow(new DuplicateKeyException("dup"));
        // 应当不抛
        service.onOrderEvent(base(OrderEvent.Type.CREATED));
    }

    @Test
    void event_without_seller_is_skipped() {
        OrderEvent e = base(OrderEvent.Type.CREATED);
        e.setSellerId(null);
        service.onOrderEvent(e);
        verify(mapper, never()).insert(any(Notification.class));
    }

    @Test
    void null_event_is_ignored() {
        service.onOrderEvent(null);
        verify(mapper, never()).insert(any(Notification.class));
    }
}
