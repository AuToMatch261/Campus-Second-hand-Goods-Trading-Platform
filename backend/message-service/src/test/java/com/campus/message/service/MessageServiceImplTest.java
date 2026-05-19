package com.campus.message.service;

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
import com.campus.message.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    ConversationMapper conversationMapper;

    @Mock
    MessageMapper messageMapper;

    @Mock
    NotificationMapper notificationMapper;

    MessageServiceImpl service;

    @BeforeEach
    void setup() {
        service = new MessageServiceImpl(conversationMapper, messageMapper, notificationMapper);
    }

    private SendMessageRequest req(long toUserId, String content) {
        SendMessageRequest r = new SendMessageRequest();
        r.setToUserId(toUserId);
        r.setContent(content);
        return r;
    }

    @Test
    void send_creates_conversation_when_absent_and_normalizes_pair() {
        when(conversationMapper.findByPair(3L, 7L)).thenReturn(null);
        when(conversationMapper.insert(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(100L);
            return 1;
        });
        when(messageMapper.insert(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(500L);
            return 1;
        });

        // fromUserId=7 toUserId=3 → normalized userA=3 userB=7
        service.send(7L, req(3L, "hi"));

        ArgumentCaptor<Conversation> cv = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).insert(cv.capture());
        assertThat(cv.getValue().getUserA()).isEqualTo(3L);
        assertThat(cv.getValue().getUserB()).isEqualTo(7L);

        // toUserId=3 == userA → 给 A 增加未读
        verify(conversationMapper).applyNewMessage(eq(100L), eq(500L), eq("hi"),
                any(LocalDateTime.class), eq("A"));
    }

    @Test
    void send_reuses_existing_conversation() {
        Conversation existing = new Conversation();
        existing.setId(100L);
        existing.setUserA(3L);
        existing.setUserB(7L);
        when(conversationMapper.findByPair(3L, 7L)).thenReturn(existing);
        when(messageMapper.insert(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(501L);
            return 1;
        });

        // fromUserId=3 → toUserId=7 是 user_b → unread_for_b +1
        service.send(3L, req(7L, "hello"));

        verify(conversationMapper, never()).insert(any(Conversation.class));
        verify(conversationMapper).applyNewMessage(eq(100L), eq(501L), eq("hello"),
                any(LocalDateTime.class), eq("B"));
    }

    @Test
    void send_rejects_self() {
        assertThatThrownBy(() -> service.send(7L, req(7L, "hi")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.MESSAGE_TO_SELF.getCode());
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void send_rejects_blank_content() {
        assertThatThrownBy(() -> service.send(7L, req(3L, "  ")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.MESSAGE_CONTENT_EMPTY.getCode());
    }

    @Test
    void send_truncates_preview_to_100_chars() {
        when(conversationMapper.findByPair(anyLong(), anyLong())).thenAnswer(inv -> {
            Conversation c = new Conversation();
            c.setId(1L);
            c.setUserA(inv.getArgument(0));
            c.setUserB(inv.getArgument(1));
            return c;
        });
        when(messageMapper.insert(any(Message.class))).thenAnswer(inv -> {
            ((Message) inv.getArgument(0)).setId(2L);
            return 1;
        });

        String longText = "x".repeat(250);
        service.send(3L, req(7L, longText));

        ArgumentCaptor<String> preview = ArgumentCaptor.forClass(String.class);
        verify(conversationMapper).applyNewMessage(anyLong(), anyLong(), preview.capture(),
                any(LocalDateTime.class), anyString());
        assertThat(preview.getValue()).hasSize(100);
    }

    @Test
    void listConversations_maps_peer_and_unread() {
        Conversation c1 = new Conversation();
        c1.setId(1L);
        c1.setUserA(3L);
        c1.setUserB(7L);
        c1.setUnreadForA(0);
        c1.setUnreadForB(5);
        c1.setLastMessageText("hi");
        c1.setLastMessageAt(LocalDateTime.now());
        Conversation c2 = new Conversation();
        c2.setId(2L);
        c2.setUserA(7L);
        c2.setUserB(9L);
        c2.setUnreadForA(2);
        c2.setUnreadForB(0);
        when(conversationMapper.selectList(any())).thenReturn(List.of(c1, c2));

        List<ConversationVO> r = service.listConversations(7L);
        assertThat(r).hasSize(2);
        // c1: user 7 是 B，对端是 A=3，未读看 B → 5
        assertThat(r.get(0).getPeerId()).isEqualTo(3L);
        assertThat(r.get(0).getUnread()).isEqualTo(5);
        // c2: user 7 是 A，对端是 B=9，未读看 A → 2
        assertThat(r.get(1).getPeerId()).isEqualTo(9L);
        assertThat(r.get(1).getUnread()).isEqualTo(2);
    }

    @Test
    void listMessages_returns_empty_when_no_conversation() {
        when(conversationMapper.findByPair(3L, 7L)).thenReturn(null);
        PageResult<MessageVO> r = service.listMessages(7L, 3L, 1, 20);
        assertThat(r.getTotal()).isZero();
        verify(conversationMapper, never()).clearUnread(anyLong(), anyString());
    }

    @Test
    void listMessages_clears_unread_for_my_side() {
        Conversation conv = new Conversation();
        conv.setId(100L);
        conv.setUserA(3L);
        conv.setUserB(7L);
        when(conversationMapper.findByPair(3L, 7L)).thenReturn(conv);

        Page<Message> page = new Page<>(1, 20);
        Message m = new Message();
        m.setId(1L);
        m.setConversationId(100L);
        m.setFromId(3L);
        m.setToId(7L);
        m.setContent("hi");
        page.setRecords(List.of(m));
        page.setTotal(1);
        when(messageMapper.selectPage(any(), any())).thenReturn(page);

        // userId=7 是 B
        service.listMessages(7L, 3L, 1, 20);

        verify(conversationMapper).clearUnread(100L, "B");
    }

    @Test
    void countUnread_sums_messages_and_notifications() {
        when(conversationMapper.sumUnread(7L)).thenReturn(13L);
        when(notificationMapper.countUnread(7L)).thenReturn(4L);
        assertThat(service.countUnread(7L)).isEqualTo(17L);
    }
}
