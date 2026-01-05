package com.example.housebackend.dto.statistics;

import java.util.List;

public record StatisticsResponse(RentalTrendStats rentalTrendLastMonth,
                                 List<DistributionItem> subwayDistribution,
                                 List<DistributionItem> regionDistribution) {
}
