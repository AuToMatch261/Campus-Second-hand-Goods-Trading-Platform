package com.campus.message.service;

import com.campus.common.response.PageResult;
import com.campus.message.dto.ConversationVO;
import com.campus.message.dto.MessageVO;
import com.campus.message.dto.SendMessageRequest;

import java.util.List;

public interface MessageService {

    MessageVO send(long fromUserId, SendMessageRequest req);

    List<ConversationVO> listConversations(long userId);

    /** 返回与对端的分页消息，并把本端未读清零 */
    PageResult<MessageVO> listMessages(long userId, long peerId, long page, long size);

    long countUnread(long userId);
}
