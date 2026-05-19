package com.campus.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.PageResult;
import com.campus.common.response.ResultCode;
import com.campus.message.dto.ConversationVO;
import com.campus.message.dto.MessageVO;
import com.campus.message.dto.SendMessageRequest;
import com.campus.message.entity.Conversation;
import com.campus.message.entity.Message;
import com.campus.message.mapper.ConversationMapper;
import com.campus.message.mapper.MessageMapper;
import com.campus.message.mapper.NotificationMapper;
import com.campus.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int PREVIEW_MAX = 100;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public MessageVO send(long fromUserId, SendMessageRequest req) {
        long toUserId = req.getToUserId();
        if (fromUserId == toUserId) {
            throw new BusinessException(ResultCode.MESSAGE_TO_SELF);
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(ResultCode.MESSAGE_CONTENT_EMPTY);
        }

        long userA = Math.min(fromUserId, toUserId);
        long userB = Math.max(fromUserId, toUserId);
        Conversation conv = conversationMapper.findByPair(userA, userB);
        if (conv == null) {
            conv = new Conversation();
            conv.setUserA(userA);
            conv.setUserB(userB);
            conv.setUnreadForA(0);
            conv.setUnreadForB(0);
            conversationMapper.insert(conv);
        }

        Message msg = new Message();
        msg.setConversationId(conv.getId());
        msg.setFromId(fromUserId);
        msg.setToId(toUserId);
        msg.setContent(req.getContent());
        msg.setRelatedProductId(req.getRelatedProductId());
        messageMapper.insert(msg);

        String preview = req.getContent().length() <= PREVIEW_MAX
                ? req.getContent()
                : req.getContent().substring(0, PREVIEW_MAX);
        // 接收方是 A 还是 B
        String whichSide = toUserId == userA ? "A" : "B";
        conversationMapper.applyNewMessage(conv.getId(), msg.getId(), preview,
                LocalDateTime.now(), whichSide);

        log.info("发送私信: convId={} from={} to={} preview='{}'", conv.getId(), fromUserId, toUserId,
                preview.length() > 30 ? preview.substring(0, 30) + "..." : preview);

        return toVO(msg);
    }

    @Override
    public List<ConversationVO> listConversations(long userId) {
        LambdaQueryWrapper<Conversation> w = new LambdaQueryWrapper<Conversation>()
                .and(q -> q.eq(Conversation::getUserA, userId).or().eq(Conversation::getUserB, userId))
                .orderByDesc(Conversation::getLastMessageAt);
        List<Conversation> all = conversationMapper.selectList(w);
        return all.stream().map(c -> toConversationVO(c, userId)).toList();
    }

    @Override
    @Transactional
    public PageResult<MessageVO> listMessages(long userId, long peerId, long page, long size) {
        long userA = Math.min(userId, peerId);
        long userB = Math.max(userId, peerId);
        Conversation conv = conversationMapper.findByPair(userA, userB);
        if (conv == null) {
            // 还没消息：直接返回空，不报错
            return PageResult.of(page, size, 0, List.of());
        }
        // 当前用户在哪一侧
        String mySide = userId == conv.getUserA() ? "A" : "B";

        // 清零本端未读
        conversationMapper.clearUnread(conv.getId(), mySide);

        LambdaQueryWrapper<Message> mw = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conv.getId())
                .orderByDesc(Message::getCreatedAt);
        Page<Message> p = messageMapper.selectPage(new Page<>(page, size), mw);
        return PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(MessageServiceImpl::toVO).toList());
    }

    @Override
    public long countUnread(long userId) {
        return conversationMapper.sumUnread(userId) + notificationMapper.countUnread(userId);
    }

    private static ConversationVO toConversationVO(Conversation c, long me) {
        boolean meIsA = c.getUserA() != null && c.getUserA() == me;
        long peer = meIsA ? c.getUserB() : c.getUserA();
        int unread = (meIsA ? c.getUnreadForA() : c.getUnreadForB()) == null
                ? 0
                : (meIsA ? c.getUnreadForA() : c.getUnreadForB());
        return ConversationVO.builder()
                .id(c.getId())
                .peerId(peer)
                .lastMessageId(c.getLastMessageId())
                .lastMessageText(c.getLastMessageText())
                .lastMessageAt(c.getLastMessageAt())
                .unread(unread)
                .build();
    }

    private static MessageVO toVO(Message m) {
        return MessageVO.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .fromId(m.getFromId())
                .toId(m.getToId())
                .content(m.getContent())
                .relatedProductId(m.getRelatedProductId())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
