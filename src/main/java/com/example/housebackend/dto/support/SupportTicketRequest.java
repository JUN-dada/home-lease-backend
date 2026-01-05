package com.example.housebackend.dto.support;

public record SupportTicketRequest(String subject,
                                   String category,
                                   String message) {
}
