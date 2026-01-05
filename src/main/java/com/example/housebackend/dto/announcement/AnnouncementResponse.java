package com.example.housebackend.dto.announcement;

import java.time.Instant;

public record AnnouncementResponse(Long id,
                                   String title,
                                   String content,
                                   boolean pinned,
                                   Instant createdAt,
                                   String createdBy) {
}
