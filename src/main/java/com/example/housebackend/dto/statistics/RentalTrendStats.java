package com.example.housebackend.dto.statistics;

import java.util.List;

public record RentalTrendStats(long totalOrders, List<DailyTrendStat> dailyStats) {
}
