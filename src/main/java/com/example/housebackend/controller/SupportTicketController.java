package com.example.housebackend.controller;

import com.example.housebackend.domain.support.SupportTicketStatus;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.support.SupportMessageResponse;
import com.example.housebackend.dto.support.SupportTicketRequest;
import com.example.housebackend.dto.support.SupportTicketResponse;
import com.example.housebackend.service.AuthService;
import com.example.housebackend.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "客服工单", description = "租客/房东与管理员的客服沟通")
public class SupportTicketController {

    private final AuthService authService;
    private final SupportService supportService;

    @PostMapping("/tickets")
    @Operation(summary = "创建客服工单")
    public ResponseEntity<SupportTicketResponse> createTicket(@RequestHeader("X-Auth-Token") String token,
                                                              @RequestBody SupportTicketRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER, UserRole.LANDLORD);
        return ResponseEntity.ok(DtoMapper.toSupportTicket(
                supportService.createTicket(user, request.subject(), request.category(), request.message())));
    }

    @GetMapping("/tickets")
    @Operation(summary = "我的客服工单")
    public ResponseEntity<Page<SupportTicketResponse>> myTickets(@RequestHeader("X-Auth-Token") String token,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER, UserRole.LANDLORD);
        Page<SupportTicketResponse> responses = supportService.listForUser(user.getId(), PageRequest.of(page, size))
                .map(DtoMapper::toSupportTicket);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/tickets")
    @Operation(summary = "客服工单列表（管理员）")
    public ResponseEntity<Page<SupportTicketResponse>> adminTickets(@RequestHeader("X-Auth-Token") String token,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    @RequestParam(required = false) SupportTicketStatus status) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        Page<SupportTicketResponse> responses = supportService
                .listForAdmin(status, PageRequest.of(page, size))
                .map(DtoMapper::toSupportTicket);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/tickets/{ticketId}/messages")
    @Operation(summary = "工单消息记录")
    public ResponseEntity<Page<SupportMessageResponse>> ticketMessages(@RequestHeader("X-Auth-Token") String token,
                                                                       @PathVariable Long ticketId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "50") int size) {
        User user = authService.requireUser(token);
        Page<SupportMessageResponse> responses = supportService
                .listMessages(ticketId, user, PageRequest.of(page, size))
                .map(DtoMapper::toSupportMessage);
        return ResponseEntity.ok(responses);
    }

    private void ensureRole(User user, UserRole... roles) {
        for (UserRole role : roles) {
            if (user.getRole() == role) {
                return;
            }
        }
        throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
    }
}
