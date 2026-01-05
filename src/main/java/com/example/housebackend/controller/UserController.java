package com.example.housebackend.controller;

import com.example.housebackend.domain.common.MediaType;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserMedia;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.auth.AuthRequest;
import com.example.housebackend.dto.user.ChangePasswordRequest;
import com.example.housebackend.dto.user.UserMediaRequest;
import com.example.housebackend.dto.user.UserProfileResponse;
import com.example.housebackend.dto.user.UserProfileUpdateRequest;
import com.example.housebackend.service.AuthService;
import com.example.housebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "提供用户资料维护与后台用户管理接口")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取个人资料", description = "当前登录用户查看自己的详细资料")
    public ResponseEntity<UserProfileResponse> me(@RequestHeader("X-Auth-Token") String token) {
        User user = authService.requireUser(token);
        return ResponseEntity.ok(DtoMapper.toProfile(user));
    }

    @PutMapping("/me")
    @Operation(summary = "更新个人资料", description = "当前用户更新自己的基础信息")
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestHeader("X-Auth-Token") String token,
                                                             @RequestBody UserProfileUpdateRequest request) {
        User user = authService.requireUser(token);
        User updated = new User();
        updated.setFullName(request.fullName());
        updated.setAvatarUrl(request.avatarUrl());
        updated.setEmail(request.email());
        updated.setPhone(request.phone());
        updated.setGender(request.gender());
        updated.setBio(request.bio());
        updated.setIdNumber(request.idNumber());
        updated.replaceMedia(toUserMediaEntities(request.gallery()));
        User saved = userService.updateProfile(user.getId(), updated);
        return ResponseEntity.ok(DtoMapper.toProfile(saved));
    }

    @PostMapping("/me/password")
    @Operation(summary = "修改密码", description = "当前用户修改登录密码")
    public ResponseEntity<Void> changePassword(@RequestHeader("X-Auth-Token") String token,
                                               @RequestBody ChangePasswordRequest request) {
        User user = authService.requireUser(token);
        userService.changePassword(user.getId(), request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "查看用户资料", description = "登录用户可查看指定账号的公开资料信息")
    public ResponseEntity<UserProfileResponse> profile(@RequestHeader("X-Auth-Token") String token,
                                                       @PathVariable Long userId) {
        authService.requireUser(token);
        User profile = userService.getProfile(userId);
        return ResponseEntity.ok(DtoMapper.toProfile(profile));
    }

    @GetMapping
    @Operation(summary = "按角色查询用户", description = "管理员按角色筛选平台用户")
    public ResponseEntity<List<UserProfileResponse>> listUsers(@RequestHeader("X-Auth-Token") String token,
                                                               @RequestParam UserRole role) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        List<UserProfileResponse> result = userService.listByRole(role).stream()
                .map(DtoMapper::toProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "管理员根据 ID 删除用户")
    public ResponseEntity<Void> deleteUser(@RequestHeader("X-Auth-Token") String token,
                                           @PathVariable Long userId) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "管理员后台创建新用户")
    public ResponseEntity<UserProfileResponse> createUser(@RequestHeader("X-Auth-Token") String token,
                                                          @RequestBody AuthRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        User created = authService.register(
                request.username(),
                request.password(),
                request.fullName(),
                request.phone(),
                request.role());
        return ResponseEntity.ok(DtoMapper.toProfile(created));
    }

    private void ensureRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
        }
    }

    private List<UserMedia> toUserMediaEntities(List<UserMediaRequest> mediaRequests) {
        if (mediaRequests == null) {
            return Collections.emptyList();
        }
        return mediaRequests.stream()
                .map(request -> {
                    UserMedia media = new UserMedia();
                    media.setMediaType(request.type() != null ? request.type() : MediaType.IMAGE);
                    media.setUrl(request.url());
                    media.setCoverUrl(request.coverUrl());
                    media.setDescription(request.description());
                    media.setSortOrder(request.sortOrder());
                    return media;
                })
                .collect(Collectors.toList());
    }
}
