package com.example.housebackend.dto.order;

import java.time.LocalDate;

public record RentalOrderCreateRequest(Long houseId,
                                       LocalDate startDate,
                                       LocalDate endDate) {
}
