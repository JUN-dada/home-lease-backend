package com.example.housebackend.dto.support;

import java.util.List;

public record SupportMessageRequest(String content,
                                    List<String> attachmentUrls) {
}
