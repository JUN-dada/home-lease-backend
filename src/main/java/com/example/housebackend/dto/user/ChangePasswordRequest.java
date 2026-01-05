package com.example.housebackend.dto.user;

public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
