package com.example.housebackend.dto.support;

import com.example.housebackend.domain.user.UserRole;
import java.time.Instant;
import java.util.List;

public record SupportMessageResponse(Long id,
                                     Long ticketId,
                                     String ticketSubject,
                                     Long senderId,
                                     String senderName,
                                     String senderAvatar,
                                     UserRole senderRole,
                                     String content,
                                     List<String> attachmentUrls,
                                     Instant createdAt) {
}
