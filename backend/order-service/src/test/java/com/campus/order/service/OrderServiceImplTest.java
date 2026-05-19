package com.campus.order.service;

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
import com.campus.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrderMapper orderMapper;

    @Mock
    ProductInternalClient productClient;

    @Mock
    ApplicationEventPublisher events;

    OrderServiceImpl service;

    @BeforeEach
    void setup() {
        service = new OrderServiceImpl(orderMapper, productClient, events);
    }

    private ProductSnapshot snapshot(long sellerId, int status) {
        ProductSnapshot p = new ProductSnapshot();
        p.setId(10L);
        p.setSellerId(sellerId);
        p.setTitle("书");
        p.setPrice(new BigDecimal("29.90"));
        p.setStatus(status);
        p.setImages(List.of("u1"));
        return p;
    }

    private Order order(long id, long buyerId, long sellerId, OrderStatus s) {
        Order o = new Order();
        o.setId(id);
        o.setProductId(10L);
        o.setProductTitle("书");
        o.setPrice(new BigDecimal("29.90"));
        o.setBuyerId(buyerId);
        o.setSellerId(sellerId);
        o.setStatus(s);
        return o;
    }

    private CreateOrderRequest req(BigDecimal price) {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setProductId(10L);
        r.setPrice(price);
        r.setRemark("自取");
        return r;
    }

    @Test
    void create_success_calls_markSold_inserts_and_publishes_event() {
        when(productClient.get(10L)).thenReturn(Result.ok(snapshot(99L, 1)));
        when(productClient.markSold(10L)).thenReturn(Result.ok());
        when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(123L);
            return 1;
        });

        OrderVO vo = service.create(7L, req(new BigDecimal("29.90")));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(productClient).markSold(10L);
        verify(orderMapper).insert(captor.capture());

        Order inserted = captor.getValue();
        assertThat(inserted.getBuyerId()).isEqualTo(7L);
        assertThat(inserted.getSellerId()).isEqualTo(99L);
        assertThat(inserted.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(inserted.getProductImage()).isEqualTo("u1");
        assertThat(vo.getId()).isEqualTo(123L);

        ArgumentCaptor<OrderEvent> ec = ArgumentCaptor.forClass(OrderEvent.class);
        verify(events).publishEvent(ec.capture());
        OrderEvent e = ec.getValue();
        assertThat(e.getType()).isEqualTo(OrderEvent.Type.CREATED);
        assertThat(e.getOrderId()).isEqualTo(123L);
        assertThat(e.getBuyerId()).isEqualTo(7L);
        assertThat(e.getSellerId()).isEqualTo(99L);
        assertThat(e.getEventId()).isNotBlank();
    }

    @Test
    void create_rejects_buying_own_product() {
        when(productClient.get(10L)).thenReturn(Result.ok(snapshot(7L, 1)));
        assertThatThrownBy(() -> service.create(7L, req(new BigDecimal("29.90"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_BUY_OWN_PRODUCT.getCode());
        verify(productClient, never()).markSold(anyLong());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void create_rejects_not_on_shelf() {
        when(productClient.get(10L)).thenReturn(Result.ok(snapshot(99L, 2))); // SOLD
        assertThatThrownBy(() -> service.create(7L, req(new BigDecimal("29.90"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_OFF_SHELF.getCode());
        verify(productClient, never()).markSold(anyLong());
    }

    @Test
    void create_rejects_price_changed() {
        when(productClient.get(10L)).thenReturn(Result.ok(snapshot(99L, 1)));
        assertThatThrownBy(() -> service.create(7L, req(new BigDecimal("9.99"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_PRODUCT_PRICE_CHANGED.getCode());
        verify(productClient, never()).markSold(anyLong());
    }

    @Test
    void create_rejects_product_not_found() {
        when(productClient.get(10L)).thenReturn(Result.fail(ResultCode.PRODUCT_NOT_FOUND));
        assertThatThrownBy(() -> service.create(7L, req(new BigDecimal("29.90"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void create_markSold_failed_no_order_created() {
        when(productClient.get(10L)).thenReturn(Result.ok(snapshot(99L, 1)));
        when(productClient.markSold(10L)).thenReturn(Result.fail(ResultCode.PRODUCT_SOLD));

        assertThatThrownBy(() -> service.create(7L, req(new BigDecimal("29.90"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_SOLD.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void confirm_only_buyer_only_pending_publishes_event() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 7L, 99L, OrderStatus.PENDING),
                order(1L, 7L, 99L, OrderStatus.COMPLETED));

        OrderVO vo = service.confirm(7L, 1L);
        verify(orderMapper, times(1)).updateById(any(Order.class));
        assertThat(vo.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        ArgumentCaptor<OrderEvent> ec = ArgumentCaptor.forClass(OrderEvent.class);
        verify(events).publishEvent(ec.capture());
        assertThat(ec.getValue().getType()).isEqualTo(OrderEvent.Type.CONFIRMED);
        assertThat(ec.getValue().getOrderId()).isEqualTo(1L);
    }

    @Test
    void confirm_rejects_seller() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 7L, 99L, OrderStatus.PENDING));
        assertThatThrownBy(() -> service.confirm(99L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_ACCESS_DENIED.getCode());
    }

    @Test
    void confirm_rejects_non_pending() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 7L, 99L, OrderStatus.COMPLETED));
        assertThatThrownBy(() -> service.confirm(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_STATUS_ILLEGAL.getCode());
    }

    @Test
    void cancel_by_buyer_publishes_event_with_buyer_tag() {
        when(orderMapper.selectById(1L)).thenReturn(
                order(1L, 7L, 99L, OrderStatus.PENDING),
                order(1L, 7L, 99L, OrderStatus.CANCELLED));

        service.cancel(7L, 1L);
        verify(orderMapper).updateById(any(Order.class));

        ArgumentCaptor<OrderEvent> ec = ArgumentCaptor.forClass(OrderEvent.class);
        verify(events).publishEvent(ec.capture());
        assertThat(ec.getValue().getType()).isEqualTo(OrderEvent.Type.CANCELLED);
        assertThat(ec.getValue().getCancelledBy()).isEqualTo("BUYER");
    }

    @Test
    void cancel_by_seller_tagged() {
        when(orderMapper.selectById(1L)).thenReturn(
                order(1L, 7L, 99L, OrderStatus.PENDING),
                order(1L, 7L, 99L, OrderStatus.CANCELLED));
        service.cancel(99L, 1L);

        ArgumentCaptor<OrderEvent> ec = ArgumentCaptor.forClass(OrderEvent.class);
        verify(events).publishEvent(ec.capture());
        assertThat(ec.getValue().getCancelledBy()).isEqualTo("SELLER");
    }

    @Test
    void cancel_rejects_third_party() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 7L, 99L, OrderStatus.PENDING));
        assertThatThrownBy(() -> service.cancel(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_ACCESS_DENIED.getCode());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void detail_only_party() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 7L, 99L, OrderStatus.PENDING));
        assertThat(service.detail(7L, 1L)).isNotNull();
        assertThat(service.detail(99L, 1L)).isNotNull();
        assertThatThrownBy(() -> service.detail(5L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_ACCESS_DENIED.getCode());
    }

    @Test
    void listAsBuyer_filters() {
        Page<Order> page = new Page<>(1, 20);
        page.setRecords(List.of(order(1L, 7L, 99L, OrderStatus.PENDING)));
        page.setTotal(1);
        when(orderMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<OrderVO> r = service.listAsBuyer(7L, 1, 20);
        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getRecords().get(0).getBuyerId()).isEqualTo(7L);
    }
}
