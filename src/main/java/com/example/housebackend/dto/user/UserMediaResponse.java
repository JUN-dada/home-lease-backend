package com.example.housebackend.dto.user;

import com.example.housebackend.domain.common.MediaType;

public record UserMediaResponse(Long id,
                                MediaType type,
                                String url,
                                String coverUrl,
                                String description,
                                Integer sortOrder) {
}
