package com.example.housebackend.controller;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.statistics.StatisticsResponse;
import com.example.housebackend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.example.housebackend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析", description = "提供平台运营相关的数据统计接口")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "获取运营统计", description = "返回近月租赁趋势、地铁分布与区域分布等统计数据")
    public ResponseEntity<StatisticsResponse> statistics(@RequestHeader("X-Auth-Token") String token) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        return ResponseEntity.ok(new StatisticsResponse(
                statisticsService.rentalTrendLastMonth(),
                statisticsService.subwayDistribution(),
                statisticsService.regionDistribution()));
    }

    private void ensureRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
        }
    }
}
