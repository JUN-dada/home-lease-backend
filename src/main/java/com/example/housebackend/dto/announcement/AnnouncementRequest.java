package com.example.housebackend.dto.announcement;

public record AnnouncementRequest(String title, String content, boolean pinned) {
}
