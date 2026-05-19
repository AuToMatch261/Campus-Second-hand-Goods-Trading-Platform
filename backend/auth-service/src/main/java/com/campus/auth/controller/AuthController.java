package com.campus.auth.controller;

import com.campus.auth.dto.LoginRequest;
import com.campus.auth.dto.LoginResult;
import com.campus.auth.dto.RegisterRequest;
import com.campus.auth.dto.UserVO;
import com.campus.auth.service.AuthService;
import com.campus.common.response.Result;
import com.campus.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "auth", description = "认证：注册 / 登录 / 当前用户")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "获取当前用户（网关解 Token 后注入 X-User-Id）")
    @GetMapping("/me")
    public Result<UserVO> me(@CurrentUser Long userId) {
        return Result.ok(authService.getCurrentUser(userId));
    }
}
