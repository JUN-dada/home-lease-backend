package com.example.housebackend.dto.support;

import com.example.housebackend.domain.support.SupportTicketStatus;
import java.time.Instant;

public record SupportTicketResponse(Long id,
                                    String subject,
                                    String category,
                                    SupportTicketStatus status,
                                    String latestMessage,
                                    Instant createdAt,
                                    Instant updatedAt,
                                    Long requesterId,
                                    String requesterName,
                                    Long handlerId,
                                    String handlerName) {
}
