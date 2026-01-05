package com.example.housebackend.dto.user;

import com.example.housebackend.domain.user.UserRole;
import java.util.List;

public record UserProfileResponse(Long id,
                                  String username,
                                  String fullName,
                                  String avatarUrl,
                                  String email,
                                  String phone,
                                  String gender,
                                  String bio,
                                  String idNumber,
                                  UserRole role,
                                  List<UserMediaResponse> gallery) {
}
