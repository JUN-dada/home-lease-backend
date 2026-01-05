package com.example.housebackend.dto.media;

import com.example.housebackend.domain.common.MediaType;

public record UploadMediaResponse(String fileName,
                                  String relativePath,
                                  String url,
                                  MediaType mediaType) {
}
