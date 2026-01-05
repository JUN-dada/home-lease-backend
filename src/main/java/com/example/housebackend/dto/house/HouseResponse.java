package com.example.housebackend.dto.house;

import com.example.housebackend.domain.house.HouseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record HouseResponse(Long id,
                            String title,
                            String description,
                            BigDecimal rentPrice,
                            BigDecimal deposit,
                            Double area,
                            String layout,
                            String orientation,
                            String address,
                            LocalDate availableFrom,
                            Long ownerId,
                            String ownerName,
                            String ownerAvatarUrl,
                            String ownerPhone,
                            String ownerEmail,
                            String regionName,
                            String subwayLineName,
                            Set<String> amenities,
                            boolean recommended,
                            HouseStatus status,
                            List<String> images,
                            List<HouseMediaResponse> media) {
}
