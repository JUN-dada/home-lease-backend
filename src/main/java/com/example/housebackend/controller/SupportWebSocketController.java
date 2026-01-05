package com.example.housebackend.controller;

import com.example.housebackend.config.StompAuthChannelInterceptor;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.support.SupportMessageRequest;
import com.example.housebackend.dto.support.SupportMessageResponse;
import com.example.housebackend.domain.support.SupportMessage;
import com.example.housebackend.domain.support.SupportTicket;
import com.example.housebackend.service.AuthService;
import com.example.housebackend.service.SupportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SupportWebSocketController {

    private final AuthService authService;
    private final SupportService supportService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/support/{ticketId}/messages")
    public void handleSupportMessage(@DestinationVariable Long ticketId,
                                     SupportMessageRequest request,
                                     StompHeaderAccessor headerAccessor) {
        java.util.Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        String token = sessionAttributes != null
                ? (String) sessionAttributes.get(StompAuthChannelInterceptor.SESSION_TOKEN_KEY)
                : null;
        if (token == null) {
            throw new com.example.housebackend.exception.AccessDeniedException("缺少认证信息");
        }
        User sender = authService.requireUser(token);
        String content = request != null ? request.content() : null;
        List<String> attachments = request != null ? request.attachmentUrls() : null;
        SupportMessage saved = supportService.sendMessage(ticketId, sender, content, attachments);
        SupportMessageResponse response = DtoMapper.toSupportMessage(saved);
        messagingTemplate.convertAndSend("/topic/support/" + ticketId, response);
        SupportTicket ticket = saved.getTicket();
        if (ticket.getRequester() != null) {
            messagingTemplate.convertAndSend("/topic/users/" + ticket.getRequester().getId(), response);
        }
        if (ticket.getHandler() != null) {
            messagingTemplate.convertAndSend("/topic/users/" + ticket.getHandler().getId(), response);
        }
    }
}
