package com.example.housebackend.dto.contact;

import com.example.housebackend.domain.contact.ContactStatus;
import java.time.Instant;
import java.time.LocalDateTime;

public record ContactResponse(Long id,
                              Long houseId,
                              String houseTitle,
                              String tenantName,
                              String landlordName,
                              String message,
                              LocalDateTime preferredVisitTime,
                              ContactStatus status,
                              Instant handledAt,
                              String remarks) {
}
