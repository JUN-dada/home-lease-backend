package com.example.housebackend.controller;

import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.house.HouseStatus;
import com.example.housebackend.domain.house.HouseMedia;
import com.example.housebackend.domain.location.Region;
import com.example.housebackend.domain.location.SubwayLine;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.house.HouseMediaRequest;
import com.example.housebackend.dto.house.FavoriteResponse;
import com.example.housebackend.dto.house.HouseRequest;
import com.example.housebackend.dto.house.HouseResponse;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.RegionRepository;
import com.example.housebackend.repository.SubwayLineRepository;
import com.example.housebackend.service.AuthService;
import com.example.housebackend.service.HouseService;
import com.example.housebackend.domain.common.MediaType;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
@Tag(name = "房源管理", description = "管理房源的搜索、发布、收藏与推荐等功能")
public class HouseController {

    private final AuthService authService;
    private final HouseService houseService;
    private final RegionRepository regionRepository;
    private final SubwayLineRepository subwayLineRepository;

    @GetMapping("/search")
    @Operation(summary = "房源搜索", description = "按地区、地铁或状态过滤房源，并支持分页")
    public ResponseEntity<Page<HouseResponse>> search(@RequestParam(required = false) Long regionId,
                                                      @RequestParam(required = false) Long subwayId,
                                                      @RequestParam(required = false) HouseStatus status,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<House> houses = houseService.search(regionId, subwayId, status, pageable);
        return ResponseEntity.ok(houses.map(DtoMapper::toHouse));
    }

    @GetMapping("/latest")
    @Operation(summary = "最新房源", description = "获取最近发布的房源列表")
    public ResponseEntity<List<HouseResponse>> latest() {
        return ResponseEntity.ok(houseService.latestHouses().stream()
                .map(DtoMapper::toHouse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/recommended")
    @Operation(summary = "推荐房源", description = "根据推荐标记获取房源列表")
    public ResponseEntity<List<HouseResponse>> recommended() {
        return ResponseEntity.ok(houseService.recommendedHouses().stream()
                .map(DtoMapper::toHouse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/random")
    @Operation(summary = "随机推荐房源", description = "随机返回指定数量的推荐房源")
    public ResponseEntity<List<HouseResponse>> random(@RequestParam(defaultValue = "4") int size) {
        return ResponseEntity.ok(houseService.randomRecommended(size).stream()
                .map(DtoMapper::toHouse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{houseId}")
    @Operation(summary = "房源详情", description = "根据房源 ID 获取详细信息")
    public ResponseEntity<HouseResponse> detail(@PathVariable Long houseId) {
        return ResponseEntity.ok(DtoMapper.toHouse(houseService.requireHouse(houseId)));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的房源", description = "房东分页查看自己发布的房源")
    public ResponseEntity<Page<HouseResponse>> myHouses(@RequestHeader("X-Auth-Token") String token,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        Page<House> houses = houseService.listByOwner(landlord.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(houses.map(DtoMapper::toHouse));
    }

    @PostMapping
    @Operation(summary = "发布房源", description = "房东创建新的房源信息")
    public ResponseEntity<HouseResponse> createHouse(@RequestHeader("X-Auth-Token") String token,
                                                     @Valid @RequestBody HouseRequest request) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        House house = toHouseEntity(request);
        house.replaceMedia(toHouseMediaEntities(request.media()));
        House saved = houseService.createHouse(landlord.getId(), house);
        return ResponseEntity.ok(DtoMapper.toHouse(saved));
    }

    @PutMapping("/{houseId}")
    @Operation(summary = "更新房源", description = "房东修改已发布的房源信息")
    public ResponseEntity<HouseResponse> updateHouse(@RequestHeader("X-Auth-Token") String token,
                                                     @PathVariable Long houseId,
                                                     @Valid @RequestBody HouseRequest request) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        House house = toHouseEntity(request);
        house.setStatus(HouseStatus.PUBLISHED);
        house.replaceMedia(toHouseMediaEntities(request.media()));
        House updated = houseService.updateHouse(landlord.getId(), houseId, house);
        return ResponseEntity.ok(DtoMapper.toHouse(updated));
    }

    @DeleteMapping("/{houseId}")
    @Operation(summary = "删除房源", description = "房东删除自己发布的房源")
    public ResponseEntity<Void> deleteHouse(@RequestHeader("X-Auth-Token") String token,
                                            @PathVariable Long houseId) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        houseService.deleteHouse(landlord.getId(), houseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{houseId}/favorite")
    @Operation(summary = "切换收藏", description = "用户收藏或取消收藏指定房源")
    public ResponseEntity<String> toggleFavorite(@RequestHeader("X-Auth-Token") String token,
                                                 @PathVariable Long houseId) {
        User user = authService.requireUser(token);
        boolean favorited = houseService.toggleFavorite(user.getId(), houseId);
        return ResponseEntity.ok(favorited ? "收藏成功" : "已取消收藏");
    }

    @GetMapping("/favorites")
    @Operation(summary = "我的收藏", description = "用户查看自己收藏的房源列表")
    public ResponseEntity<List<FavoriteResponse>> favorites(@RequestHeader("X-Auth-Token") String token) {
        User user = authService.requireUser(token);
        return ResponseEntity.ok(houseService.listFavorites(user.getId()).stream()
                .map(DtoMapper::toFavorite)
                .collect(Collectors.toList()));
    }

    @GetMapping("/favorites/all")
    @Operation(summary = "全部收藏", description = "管理员查看平台所有收藏记录")
    public ResponseEntity<List<FavoriteResponse>> allFavorites(@RequestHeader("X-Auth-Token") String token) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        return ResponseEntity.ok(houseService.listAllFavorites().stream()
                .map(DtoMapper::toFavorite)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{houseId}/recommend")
    @Operation(summary = "设置推荐状态", description = "管理员标记房源是否为推荐房源")
    public ResponseEntity<HouseResponse> recommend(@RequestHeader("X-Auth-Token") String token,
                                                   @PathVariable Long houseId,
                                                   @RequestParam(defaultValue = "true") boolean value) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        House updated = houseService.markRecommended(houseId, value);
        return ResponseEntity.ok(DtoMapper.toHouse(updated));
    }

    @PostMapping("/{houseId}/status")
    @Operation(summary = "更新房源状态", description = "管理员更新房源的上下架状态")
    public ResponseEntity<HouseResponse> updateStatus(@RequestHeader("X-Auth-Token") String token,
                                                      @PathVariable Long houseId,
                                                      @RequestParam HouseStatus status) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        return ResponseEntity.ok(DtoMapper.toHouse(houseService.updateStatus(houseId, status)));
    }

    private House toHouseEntity(HouseRequest request) {
        House house = new House();
        house.setTitle(request.title());
        house.setDescription(request.description());
        house.setRentPrice(request.rentPrice());
        house.setDeposit(request.deposit());
        house.setArea(request.area());
        house.setLayout(request.layout());
        house.setOrientation(request.orientation());
        house.setAddress(request.address());
        house.setAvailableFrom(request.availableFrom());
        if (request.amenities() != null) {
            house.setAmenities(request.amenities());
        }
        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("地区不存在"));
            house.setRegion(region);
        }
        if (request.subwayLineId() != null) {
            SubwayLine subwayLine = subwayLineRepository.findById(request.subwayLineId())
                    .orElseThrow(() -> new ResourceNotFoundException("地铁信息不存在"));
            house.setSubwayLine(subwayLine);
        }
        house.setStatus(HouseStatus.PUBLISHED);
        return house;
    }

    private void ensureRole(User user, UserRole role) {
        if (user.getRole() != role) {
            throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
        }
    }

    private List<HouseMedia> toHouseMediaEntities(List<HouseMediaRequest> mediaRequests) {
        if (mediaRequests == null) {
            return Collections.emptyList();
        }
        return mediaRequests.stream()
                .map(request -> {
                    HouseMedia media = new HouseMedia();
                    media.setMediaType(request.type() != null ? request.type() : MediaType.IMAGE);
                    media.setUrl(request.url());
                    media.setCoverUrl(request.coverUrl());
                    media.setDescription(request.description());
                    media.setSortOrder(request.sortOrder());
                    return media;
                })
                .collect(Collectors.toList());
    }
}
