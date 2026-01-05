package com.example.housebackend.controller;

import com.example.housebackend.domain.auth.AuthToken;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.dto.auth.AuthRequest;
import com.example.housebackend.dto.auth.AuthResponse;
import com.example.housebackend.dto.auth.LoginRequest;
import com.example.housebackend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "提供用户注册、登录与注销接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册普通用户或房东账号，并立即返回登录凭证")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        if (request.role() == com.example.housebackend.domain.user.UserRole.ADMIN) {
            throw new com.example.housebackend.exception.AccessDeniedException("禁止自行注册管理员账号");
        }
        User user = authService.register(
                request.username(),
                request.password(),
                request.fullName(),
                request.phone(),
                request.role());
        AuthToken token = authService.login(request.username(), request.password());
        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                token.getToken()));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验用户名与密码，返回认证令牌")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthToken token = authService.login(request.username(), request.password());
        User user = token.getUser();
        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                token.getToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "根据令牌注销当前登录会话")
    public ResponseEntity<Void> logout(@RequestHeader("X-Auth-Token") String token) {
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
