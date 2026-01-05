package com.example.housebackend.dto.user;

import java.util.List;

public record UserProfileUpdateRequest(String fullName,
                                       String avatarUrl,
                                       String email,
                                       String phone,
                                       String gender,
                                       String bio,
                                       String idNumber,
                                       List<UserMediaRequest> gallery) {
}
