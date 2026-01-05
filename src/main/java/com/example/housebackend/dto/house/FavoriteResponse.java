package com.example.housebackend.dto.house;

public record FavoriteResponse(Long favoriteId,
                               Long houseId,
                               String houseTitle,
                               String landlordName) {
}
