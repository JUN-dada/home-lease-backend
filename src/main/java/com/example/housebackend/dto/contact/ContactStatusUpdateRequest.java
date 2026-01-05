package com.example.housebackend.dto.contact;

import com.example.housebackend.domain.contact.ContactStatus;

public record ContactStatusUpdateRequest(ContactStatus status, String remarks) {
}
