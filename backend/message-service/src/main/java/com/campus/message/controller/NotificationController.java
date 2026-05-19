package com.campus.message.controller;

import com.campus.common.response.PageResult;
import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import com.campus.message.dto.NotificationVO;
import com.campus.message.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "notification", description = "系统通知（订单事件等）")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @Operation(summary = "我的通知列表（按时间倒序）")
    @GetMapping
    public Result<PageResult<NotificationVO>> list(@CurrentUser Long userId,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size) {
        return Result.ok(service.list(userId, page, size));
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@CurrentUser Long userId) {
        return Result.ok(service.countUnread(userId));
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/{id}/read")
    public Result<Void> read(@CurrentUser Long userId, @PathVariable long id) {
        service.markRead(userId, id);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> readAll(@CurrentUser Long userId) {
        service.markAllRead(userId);
        return Result.ok();
    }
}
