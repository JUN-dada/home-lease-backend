package com.example.housebackend.dto.house;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record HouseRequest(String title,
                           String description,
                           BigDecimal rentPrice,
                           BigDecimal deposit,
                           Double area,
                           String layout,
                           String orientation,
                           String address,
                           LocalDate availableFrom,
                           Long regionId,
                           Long subwayLineId,
                           Set<String> amenities,
                           List<HouseMediaRequest> media) {
}
