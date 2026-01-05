package com.example.housebackend.service;

import com.example.housebackend.domain.common.MediaType;
import com.example.housebackend.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CHINA);

    private final Path rootLocation;

    public FileStorageService(@Value("${app.upload-dir:upload}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建上传目录: " + this.rootLocation, ex);
        }
    }

    public StoredMedia store(MultipartFile file, MediaType overrideType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择要上传的文件");
        }
        MediaType mediaType = overrideType != null ? overrideType : resolveMediaType(file);
        if (mediaType == null) {
            throw new BadRequestException("不支持的文件类型");
        }
        String filename = generateFileName(file.getOriginalFilename(), file.getContentType());
        Path destination = this.rootLocation.resolve(filename);
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("保存文件失败", ex);
        }
        String relativePath = filename;
        String url = "/uploads/" + relativePath.replace("\\", "/");
        return new StoredMedia(filename, relativePath, url, mediaType);
    }

    private MediaType resolveMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        return null;
    }

    private String generateFileName(String originalFilename, String contentType) {
        String cleanName = StringUtils.hasText(originalFilename) ? StringUtils.cleanPath(originalFilename) : "";
        String extension = "";
        int dot = cleanName.lastIndexOf('.');
        if (dot > -1 && dot < cleanName.length() - 1) {
            extension = cleanName.substring(dot);
        } else if (StringUtils.hasText(contentType)) {
            extension = guessExtension(contentType);
        }
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        String random = UUID.randomUUID().toString().replace("-", "");
        return timestamp + "-" + random + extension;
    }

    private String guessExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/quicktime" -> ".mov";
            case "video/x-msvideo" -> ".avi";
            default -> "";
        };
    }

    public record StoredMedia(String fileName, String relativePath, String url, MediaType mediaType) {
    }
}
