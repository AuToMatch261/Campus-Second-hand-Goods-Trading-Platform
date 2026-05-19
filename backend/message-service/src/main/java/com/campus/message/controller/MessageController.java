package com.campus.message.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import com.campus.message.dto.ConversationVO;
import com.campus.message.dto.MessageVO;
import com.campus.message.dto.SendMessageRequest;
import com.campus.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "message", description = "私信：发送 / 会话 / 未读")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService service;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<MessageVO> send(@CurrentUser Long userId,
                                 @Valid @RequestBody SendMessageRequest req) {
        return Result.ok(service.send(userId, req));
    }

    @Operation(summary = "我的会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations(@CurrentUser Long userId) {
        return Result.ok(service.listConversations(userId));
    }

    @Operation(summary = "与某用户的消息列表（拉取时会把本端未读清零）")
    @GetMapping("/conversations/{peerId}/messages")
    public Result<PageResult<MessageVO>> messages(@CurrentUser Long userId,
                                                  @PathVariable long peerId,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "30") long size) {
        return Result.ok(service.listMessages(userId, peerId, page, size));
    }

    @Operation(summary = "我的总未读数（顶部红点用）")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@CurrentUser Long userId) {
        return Result.ok(service.countUnread(userId));
    }
}
