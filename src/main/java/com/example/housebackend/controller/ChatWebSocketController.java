package com.example.housebackend.controller;

import com.example.housebackend.config.StompAuthChannelInterceptor;
import com.example.housebackend.domain.chat.ChatMessage;
import com.example.housebackend.domain.contact.ContactRecord;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.chat.ChatMessageRequest;
import com.example.housebackend.dto.chat.ChatMessageResponse;
import com.example.housebackend.service.AuthService;
import com.example.housebackend.service.ChatService;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final AuthService authService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/contacts/{contactId}/messages")
    public void handleMessage(@DestinationVariable Long contactId,
                              ChatMessageRequest request,
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
        List<String> imageUrls = request != null ? request.imageUrls() : null;
        ChatMessage savedMessage = chatService.sendMessage(contactId, sender, content, imageUrls);
        ChatMessageResponse response = DtoMapper.toChatMessage(savedMessage);
        messagingTemplate.convertAndSend("/topic/contacts/" + contactId, response);
        broadcastToParticipants(savedMessage.getContactRecord(), response);
    }

    private void broadcastToParticipants(ContactRecord record, ChatMessageResponse response) {
        if (record == null) {
            return;
        }
        Set<Long> participantIds = new HashSet<>();
        if (record.getTenant() != null) {
            participantIds.add(record.getTenant().getId());
        }
        if (record.getLandlord() != null) {
            participantIds.add(record.getLandlord().getId());
        }
        participantIds.forEach(userId -> messagingTemplate.convertAndSend("/topic/users/" + userId, response));
    }
}
