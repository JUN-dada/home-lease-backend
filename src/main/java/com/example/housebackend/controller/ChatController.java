package com.example.housebackend.controller;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.chat.ChatMessageRequest;
import com.example.housebackend.dto.chat.ChatMessageResponse;
import com.example.housebackend.service.AuthService;
import com.example.housebackend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "聊天", description = "租客、房东与管理员之间的聊天记录")
public class ChatController {

    private final AuthService authService;
    private final ChatService chatService;

    @PostMapping("/contacts/{contactId}/messages")
    @Operation(summary = "发送聊天消息", description = "租客、房东或管理员在看房联系中发送文字或图片消息")
    public ResponseEntity<ChatMessageResponse> sendMessage(@RequestHeader("X-Auth-Token") String token,
                                                           @PathVariable Long contactId,
                                                           @RequestBody(required = false) ChatMessageRequest request) {
        User sender = authService.requireUser(token);
        String content = request != null ? request.content() : null;
        List<String> imageUrls = request != null ? request.imageUrls() : null;
        return ResponseEntity.ok(DtoMapper.toChatMessage(
                chatService.sendMessage(contactId, sender, content, imageUrls)));
    }

    @GetMapping("/contacts/{contactId}/messages")
    @Operation(summary = "查询聊天记录", description = "按时间顺序分页获取指定看房联系的聊天记录")
    public ResponseEntity<Page<ChatMessageResponse>> listMessages(@RequestHeader("X-Auth-Token") String token,
                                                                  @PathVariable Long contactId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        User requester = authService.requireUser(token);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ChatMessageResponse> responses = chatService.listMessages(contactId, requester, pageRequest)
                .map(DtoMapper::toChatMessage);
        return ResponseEntity.ok(responses);
    }
}
