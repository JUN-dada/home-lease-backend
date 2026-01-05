package com.example.housebackend.dto.chat;

import java.util.List;

public record ChatMessageRequest(String content, List<String> imageUrls) {
}
