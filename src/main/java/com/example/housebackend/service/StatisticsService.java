package com.example.housebackend.service;

import com.example.housebackend.domain.house.House;
import com.example.housebackend.dto.statistics.DailyTrendStat;
import com.example.housebackend.dto.statistics.DistributionItem;
import com.example.housebackend.dto.statistics.RentalTrendStats;
import com.example.housebackend.repository.HouseRepository;
import com.example.housebackend.repository.RentalOrderRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final RentalOrderRepository rentalOrderRepository;
    private final HouseRepository houseRepository;

    @Transactional(readOnly = true)
    public RentalTrendStats rentalTrendLastMonth() {
        List<DailyTrendStat> dailyStats = new ArrayList<>();
        long total = 0L;
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Instant start = day.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            long count = rentalOrderRepository.countByCreatedAtBetween(start, end);
            total += count;
            dailyStats.add(new DailyTrendStat(day.toString(), count));
        }
        return new RentalTrendStats(total, dailyStats);
    }

    @Transactional(readOnly = true)
    public List<DistributionItem> subwayDistribution() {
        List<House> houses = houseRepository.findAll();
        long withSubway = houses.stream().filter(h -> h.getSubwayLine() != null).count();
        long withoutSubway = houses.size() - withSubway;
        return List.of(
                new DistributionItem("靠近地铁", withSubway),
                new DistributionItem("非地铁房源", withoutSubway));
    }

    @Transactional(readOnly = true)
    public List<DistributionItem> regionDistribution() {
        Map<String, Long> counted = houseRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        house -> house.getRegion() != null ? house.getRegion().getName() : "未分配区域",
                        Collectors.counting()));
        return counted.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(entry -> new DistributionItem(entry.getKey(), entry.getValue()))
                .toList();
    }
}
