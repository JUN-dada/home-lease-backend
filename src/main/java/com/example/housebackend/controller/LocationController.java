package com.example.housebackend.controller;

import com.example.housebackend.domain.location.Region;
import com.example.housebackend.domain.location.SubwayLine;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.location.RegionRequest;
import com.example.housebackend.dto.location.RegionResponse;
import com.example.housebackend.dto.location.SubwayRequest;
import com.example.housebackend.dto.location.SubwayResponse;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.RegionRepository;
import com.example.housebackend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.example.housebackend.service.LocationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "地区与地铁", description = "维护地区与地铁线路的基础数据")
public class LocationController {

    private final LocationService locationService;
    private final AuthService authService;
    private final RegionRepository regionRepository;

    @GetMapping("/regions")
    @Operation(summary = "查询地区列表", description = "获取全部房源可用地区信息")
    public ResponseEntity<List<RegionResponse>> regions() {
        List<RegionResponse> responses = locationService.listRegions().stream()
                .map(DtoMapper::toRegion)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/regions")
    @Operation(summary = "创建地区", description = "管理员新增一个房源地区")
    public ResponseEntity<RegionResponse> createRegion(@RequestHeader("X-Auth-Token") String token,
                                                       @RequestBody RegionRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        Region region = new Region();
        region.setName(request.name());
        region.setDescription(request.description());
        return ResponseEntity.ok(DtoMapper.toRegion(locationService.createRegion(region)));
    }

    @PutMapping("/regions/{regionId}")
    @Operation(summary = "更新地区", description = "管理员根据 ID 修改地区信息")
    public ResponseEntity<RegionResponse> updateRegion(@RequestHeader("X-Auth-Token") String token,
                                                       @PathVariable Long regionId,
                                                       @RequestBody RegionRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        Region region = new Region();
        region.setName(request.name());
        region.setDescription(request.description());
        return ResponseEntity.ok(DtoMapper.toRegion(locationService.updateRegion(regionId, region)));
    }

    @DeleteMapping("/regions/{regionId}")
    @Operation(summary = "删除地区", description = "管理员根据 ID 删除地区")
    public ResponseEntity<Void> deleteRegion(@RequestHeader("X-Auth-Token") String token,
                                             @PathVariable Long regionId) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        locationService.deleteRegion(regionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subways")
    @Operation(summary = "查询地铁线路", description = "获取全部地铁线路及车站信息")
    public ResponseEntity<List<SubwayResponse>> subways() {
        List<SubwayResponse> responses = locationService.listSubway().stream()
                .map(DtoMapper::toSubway)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/subways")
    @Operation(summary = "创建地铁线路", description = "管理员新增地铁线路与车站")
    public ResponseEntity<SubwayResponse> createSubway(@RequestHeader("X-Auth-Token") String token,
                                                       @RequestBody SubwayRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        SubwayLine subwayLine = new SubwayLine();
        subwayLine.setLineName(request.lineName());
        subwayLine.setStationName(request.stationName());
        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("地区不存在"));
            subwayLine.setRegion(region);
        }
        return ResponseEntity.ok(DtoMapper.toSubway(locationService.createSubway(subwayLine)));
    }

    @PutMapping("/subways/{subwayId}")
    @Operation(summary = "更新地铁线路", description = "管理员根据 ID 修改地铁线路与车站信息")
    public ResponseEntity<SubwayResponse> updateSubway(@RequestHeader("X-Auth-Token") String token,
                                                       @PathVariable Long subwayId,
                                                       @RequestBody SubwayRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        SubwayLine subwayLine = new SubwayLine();
        subwayLine.setLineName(request.lineName());
        subwayLine.setStationName(request.stationName());
        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("地区不存在"));
            subwayLine.setRegion(region);
        }
        return ResponseEntity.ok(DtoMapper.toSubway(locationService.updateSubway(subwayId, subwayLine)));
    }

    @DeleteMapping("/subways/{subwayId}")
    @Operation(summary = "删除地铁线路", description = "管理员根据 ID 删除地铁线路")
    public ResponseEntity<Void> deleteSubway(@RequestHeader("X-Auth-Token") String token,
                                             @PathVariable Long subwayId) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        locationService.deleteSubway(subwayId);
        return ResponseEntity.noContent().build();
    }

    private void ensureRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
        }
    }
}
