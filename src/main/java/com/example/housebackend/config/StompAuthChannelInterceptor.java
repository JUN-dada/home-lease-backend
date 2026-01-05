package com.example.housebackend.config;

import com.example.housebackend.exception.AccessDeniedException;
import com.example.housebackend.service.AuthService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String SESSION_TOKEN_KEY = "chatToken";

    private final AuthService authService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (!StringUtils.hasText(token)) {
                throw new AccessDeniedException("缺少认证信息");
            }
            authService.requireUser(token);
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(SESSION_TOKEN_KEY, token);
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("X-Auth-Token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        Object headerToken = accessor.getHeader("X-Auth-Token");
        if (headerToken instanceof String header) {
            return header;
        }
        return null;
    }
}
