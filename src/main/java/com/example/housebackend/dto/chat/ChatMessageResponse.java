package com.example.housebackend.dto.chat;

import com.example.housebackend.domain.user.UserRole;
import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(Long id,
                                  Long contactId,
                                  Long senderId,
                                  String senderName,
                                  String senderAvatar,
                                  UserRole senderRole,
                                  String content,
                                  List<String> imageUrls,
                                  Instant createdAt,
                                  Long contactHouseId,
                                  String contactHouseTitle,
                                  String contactTenantName,
                                  String contactLandlordName) {
}
