package com.example.housebackend.controller;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.announcement.AnnouncementRequest;
import com.example.housebackend.dto.announcement.AnnouncementResponse;
import com.example.housebackend.service.AnnouncementService;
import com.example.housebackend.service.AuthService;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "系统公告", description = "管理平台公告的发布、更新与删除")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "分页查询公告", description = "按页获取系统公告列表")
    public ResponseEntity<Page<AnnouncementResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Page<AnnouncementResponse> responses = announcementService.list(PageRequest.of(page, size))
                .map(DtoMapper::toAnnouncement);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新公告", description = "返回置顶及最新发布的公告列表")
    public ResponseEntity<List<AnnouncementResponse>> latest() {
        List<AnnouncementResponse> responses = announcementService.latest().stream()
                .map(DtoMapper::toAnnouncement)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    @Operation(summary = "创建公告", description = "管理员创建新的系统公告")
    public ResponseEntity<AnnouncementResponse> create(@RequestHeader("X-Auth-Token") String token,
                                                       @RequestBody AnnouncementRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        var announcement = new com.example.housebackend.domain.announcement.SystemAnnouncement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setPinned(request.pinned());
        return ResponseEntity.ok(DtoMapper.toAnnouncement(announcementService.create(admin.getId(), announcement)));
    }

    @PutMapping("/{announcementId}")
    @Operation(summary = "更新公告", description = "管理员根据 ID 修改公告内容")
    public ResponseEntity<AnnouncementResponse> update(@RequestHeader("X-Auth-Token") String token,
                                                       @PathVariable Long announcementId,
                                                       @RequestBody AnnouncementRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        var announcement = new com.example.housebackend.domain.announcement.SystemAnnouncement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setPinned(request.pinned());
        return ResponseEntity.ok(DtoMapper.toAnnouncement(announcementService.update(admin.getId(), announcementId, announcement)));
    }

    @DeleteMapping("/{announcementId}")
    @Operation(summary = "删除公告", description = "管理员根据 ID 删除系统公告")
    public ResponseEntity<Void> delete(@RequestHeader("X-Auth-Token") String token,
                                       @PathVariable Long announcementId) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        announcementService.delete(announcementId);
        return ResponseEntity.noContent().build();
    }

    private void ensureRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
        }
    }
}
