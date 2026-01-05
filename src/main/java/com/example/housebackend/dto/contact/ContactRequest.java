package com.example.housebackend.dto.contact;

import java.time.LocalDateTime;

public record ContactRequest(Long houseId,
                             String message,
                             LocalDateTime preferredVisitTime) {
}
