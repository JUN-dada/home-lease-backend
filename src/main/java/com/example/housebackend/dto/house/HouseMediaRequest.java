package com.example.housebackend.dto.house;

import com.example.housebackend.domain.common.MediaType;

public record HouseMediaRequest(Long id,
                                MediaType type,
                                String url,
                                String coverUrl,
                                String description,
                                Integer sortOrder) {
}
