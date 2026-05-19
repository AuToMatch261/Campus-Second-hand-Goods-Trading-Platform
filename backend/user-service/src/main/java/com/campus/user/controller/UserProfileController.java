package com.campus.user.controller;

import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import com.campus.user.dto.ProfileVO;
import com.campus.user.dto.UpdateProfileRequest;
import com.campus.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "profile", description = "用户资料")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService service;

    @Operation(summary = "获取当前用户资料")
    @GetMapping("/me")
    public Result<ProfileVO> me(@CurrentUser Long userId) {
        return Result.ok(service.getById(userId));
    }

    @Operation(summary = "更新当前用户资料（只更新非 null 字段）")
    @PutMapping("/me")
    public Result<ProfileVO> updateMe(@CurrentUser Long userId,
                                     @Valid @RequestBody UpdateProfileRequest req) {
        return Result.ok(service.updateMyProfile(userId, req));
    }

    @Operation(summary = "获取指定用户的公开资料")
    @GetMapping("/{userId}")
    public Result<ProfileVO> getById(@Parameter(description = "目标用户 ID") @PathVariable long userId) {
        return Result.ok(service.getById(userId));
    }
}
