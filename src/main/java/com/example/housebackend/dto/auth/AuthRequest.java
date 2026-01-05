package com.example.housebackend.dto.auth;

import com.example.housebackend.domain.user.UserRole;

public record AuthRequest(String username,
                          String password,
                          String fullName,
                          String phone,
                          UserRole role) {
}
