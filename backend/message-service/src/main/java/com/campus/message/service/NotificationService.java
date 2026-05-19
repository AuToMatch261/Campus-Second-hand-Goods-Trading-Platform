package com.campus.message.service;

import com.campus.common.mq.event.OrderEvent;
import com.campus.common.response.PageResult;
import com.campus.message.dto.NotificationVO;

public interface NotificationService {

    PageResult<NotificationVO> list(long userId, long page, long size);

    long countUnread(long userId);

    void markRead(long userId, long id);

    void markAllRead(long userId);

    /** 订单事件转通知，幂等（按 event_id + user_id 去重） */
    void onOrderEvent(OrderEvent event);
}
