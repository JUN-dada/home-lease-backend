package com.example.housebackend.dto.order;

import com.example.housebackend.domain.order.OrderTerminationStatus;
import com.example.housebackend.domain.order.RentalOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RentalOrderResponse(Long id,
                                  Long houseId,
                                  String houseTitle,
                                  String tenantName,
                                  String landlordName,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  BigDecimal monthlyRent,
                                  BigDecimal deposit,
                                  RentalOrderStatus status,
                                  String contractUrl,
                                  OrderTerminationStatus terminationStatus,
                                  String terminationReason,
                                  String terminationFeedback,
                                  Instant terminationRequestedAt,
                                  Instant terminationResolvedAt,
                                  String terminationRequesterName,
                                  String terminationResolverName) {
}
