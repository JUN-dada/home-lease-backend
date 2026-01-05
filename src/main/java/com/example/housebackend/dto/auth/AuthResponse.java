package com.example.housebackend.dto.auth;

import com.example.housebackend.domain.user.UserRole;

public record AuthResponse(Long userId,
                           String username,
                           String fullName,
                           UserRole role,
                           String token) {
}
