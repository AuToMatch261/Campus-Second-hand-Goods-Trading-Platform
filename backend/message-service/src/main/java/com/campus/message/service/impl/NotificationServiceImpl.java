package com.campus.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.mq.event.OrderEvent;
import com.campus.common.response.PageResult;
import com.campus.message.dto.NotificationVO;
import com.campus.message.entity.Notification;
import com.campus.message.mapper.NotificationMapper;
import com.campus.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public PageResult<NotificationVO> list(long userId, long page, long size) {
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        Page<Notification> p = notificationMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(NotificationServiceImpl::toVO).toList());
    }

    @Override
    public long countUnread(long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markRead(long userId, long id) {
        notificationMapper.markRead(userId, id);
    }

    @Override
    public void markAllRead(long userId) {
        notificationMapper.markAllRead(userId);
    }

    @Override
    public void onOrderEvent(OrderEvent event) {
        if (event == null || event.getEventId() == null || event.getType() == null) {
            log.warn("收到非法订单事件: {}", event);
            return;
        }
        switch (event.getType()) {
            case CREATED -> insertSafe(event, event.getSellerId(),
                    "ORDER_CREATED",
                    "你的商品被下单",
                    String.format("买家已下单：《%s》，请尽快联系买家完成交易。", safeTitle(event)));
            case CONFIRMED -> insertSafe(event, event.getSellerId(),
                    "ORDER_CONFIRMED",
                    "买家已确认收货",
                    String.format("订单已完成：《%s》。", safeTitle(event)));
            case CANCELLED -> {
                boolean cancelledByBuyer = "BUYER".equalsIgnoreCase(event.getCancelledBy());
                long notifyUser = cancelledByBuyer ? event.getSellerId() : event.getBuyerId();
                String title = cancelledByBuyer ? "买家已取消订单" : "卖家已取消订单";
                String content = String.format("订单已取消：《%s》。商品已恢复上架。", safeTitle(event));
                insertSafe(event, notifyUser, "ORDER_CANCELLED", title, content);
            }
        }
    }

    private void insertSafe(OrderEvent event, Long userId, String type, String title, String content) {
        if (userId == null) {
            log.warn("订单事件无目标用户，跳过通知: eventId={} type={}", event.getEventId(), type);
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRefType("ORDER");
        n.setRefId(event.getOrderId());
        n.setEventId(event.getEventId());
        try {
            notificationMapper.insert(n);
            log.info("已生成系统通知: userId={} type={} orderId={} eventId={}",
                    userId, type, event.getOrderId(), event.getEventId());
        } catch (DuplicateKeyException e) {
            log.info("订单事件已处理过，跳过: eventId={} userId={}", event.getEventId(), userId);
        }
    }

    private static String safeTitle(OrderEvent event) {
        return event.getProductTitle() == null ? "" : event.getProductTitle();
    }

    private static NotificationVO toVO(Notification n) {
        return NotificationVO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .refType(n.getRefType())
                .refId(n.getRefId())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
