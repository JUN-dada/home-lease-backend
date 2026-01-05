package com.example.housebackend.controller;

import com.example.housebackend.domain.common.MediaType;
import com.example.housebackend.dto.media.UploadMediaResponse;
import com.example.housebackend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "媒体上传", description = "上传图片或视频文件并返回可访问地址")
public class MediaController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传媒体文件", description = "上传图片或视频，并返回可直接在媒体列表中使用的地址")
    public ResponseEntity<UploadMediaResponse> upload(@RequestPart("file") MultipartFile file,
                                                      @RequestParam(value = "type", required = false) MediaType mediaType) {
        FileStorageService.StoredMedia stored = fileStorageService.store(file, mediaType);
        UploadMediaResponse response = new UploadMediaResponse(
                stored.fileName(),
                stored.relativePath(),
                stored.url(),
                stored.mediaType());
        return ResponseEntity.ok(response);
    }
}
