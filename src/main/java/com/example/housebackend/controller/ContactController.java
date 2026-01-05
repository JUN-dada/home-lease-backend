package com.example.housebackend.controller;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.contact.ContactRequest;
import com.example.housebackend.dto.contact.ContactResponse;
import com.example.housebackend.dto.contact.ContactStatusUpdateRequest;
import com.example.housebackend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.example.housebackend.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@Tag(name = "联系方式管理", description = "处理租客与房东之间的联系请求与状态更新")
public class ContactController {

    private final AuthService authService;
    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "提交看房请求", description = "租客提交与房东的联系与看房需求")
    public ResponseEntity<ContactResponse> create(@RequestHeader("X-Auth-Token") String token,
                                                  @RequestBody ContactRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER);
        return ResponseEntity.ok(DtoMapper.toContact(contactService.createRecord(
                user.getId(),
                request.houseId(),
                request.message(),
                request.preferredVisitTime())));
    }

    @PostMapping("/ensure")
    @Operation(summary = "确保存在联系记录", description = "租客在发起聊天前确保生成联系记录，避免重复创建")
    public ResponseEntity<ContactResponse> ensureContact(@RequestHeader("X-Auth-Token") String token,
                                                         @RequestBody ContactRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER);
        return ResponseEntity.ok(DtoMapper.toContact(contactService.ensureConversation(
                user.getId(),
                request.houseId(),
                request.message(),
                request.preferredVisitTime())));
    }

    @GetMapping("/mine")
    @Operation(summary = "租客查看联系记录", description = "按页查询当前租客发起的看房请求")
    public ResponseEntity<Page<ContactResponse>> myContacts(@RequestHeader("X-Auth-Token") String token,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER);
        Page<ContactResponse> responses = contactService.listForTenant(user.getId(), PageRequest.of(page, size))
                .map(DtoMapper::toContact);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/landlord")
    @Operation(summary = "房东查看联系记录", description = "按页查询当前房东收到的租客请求")
    public ResponseEntity<Page<ContactResponse>> landlordContacts(@RequestHeader("X-Auth-Token") String token,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        Page<ContactResponse> responses = contactService.listForLandlord(landlord.getId(), PageRequest.of(page, size))
                .map(DtoMapper::toContact);
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "管理员查看全部联系记录", description = "按页查询平台全部看房请求记录")
    public ResponseEntity<Page<ContactResponse>> allContacts(@RequestHeader("X-Auth-Token") String token,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        Page<ContactResponse> responses = contactService.listForAdmin(PageRequest.of(page, size))
                .map(DtoMapper::toContact);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{recordId}/status")
    @Operation(summary = "房东更新请求状态", description = "房东对指定联系记录更新处理状态与备注")
    public ResponseEntity<ContactResponse> updateStatus(@RequestHeader("X-Auth-Token") String token,
                                                        @PathVariable Long recordId,
                                                        @RequestBody ContactStatusUpdateRequest request) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        return ResponseEntity.ok(DtoMapper.toContact(contactService.updateStatus(
                landlord.getId(), recordId, request.status(), request.remarks())));
    }

    private void ensureRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
        }
    }
}
