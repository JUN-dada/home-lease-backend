package com.example.housebackend.service;

import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.house.HouseFavorite;
import com.example.housebackend.domain.house.HouseStatus;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.exception.AccessDeniedException;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.HouseFavoriteRepository;
import com.example.housebackend.repository.HouseRepository;
import com.example.housebackend.repository.ContactRecordRepository;
import com.example.housebackend.repository.RentalOrderRepository;
import com.example.housebackend.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository houseRepository;
    private final HouseFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ContactRecordRepository contactRecordRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final CertificationService certificationService;
    private final Random random = new Random();

    @Transactional
    public House createHouse(Long ownerId, House house) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("房东不存在"));
        if (!certificationService.isCertifiedLandlord(ownerId)) {
            throw new BadRequestException("房东尚未通过认证，无法发布房源");
        }
        house.setOwner(owner);
        if (house.getDeposit() == null) {
            house.setDeposit(house.getRentPrice());
        }
        house.setStatus(HouseStatus.PUBLISHED);
        return houseRepository.save(house);
    }

    @Transactional
    public House updateHouse(Long ownerId, Long houseId, House incoming) {
        House house = requireHouse(houseId);
        if (!house.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("无权修改该房源");
        }
        house.setTitle(incoming.getTitle());
        house.setDescription(incoming.getDescription());
        house.setRentPrice(incoming.getRentPrice());
        house.setDeposit(incoming.getDeposit() != null ? incoming.getDeposit() : incoming.getRentPrice());
        house.setArea(incoming.getArea());
        house.setLayout(incoming.getLayout());
        house.setOrientation(incoming.getOrientation());
        house.setAddress(incoming.getAddress());
        house.setAvailableFrom(incoming.getAvailableFrom());
        house.setAmenities(incoming.getAmenities());
        house.setRegion(incoming.getRegion());
        house.setSubwayLine(incoming.getSubwayLine());
        house.setStatus(incoming.getStatus());
        house.replaceMedia(incoming.getMedia());
        return houseRepository.save(house);
    }

    @Transactional(readOnly = true)
    public House requireHouse(Long houseId) {
        return houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
    }

    @Transactional(readOnly = true)
    public Page<House> search(Long regionId, Long subwayId, HouseStatus status, Pageable pageable) {
        return houseRepository.search(regionId, subwayId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<House> listByOwner(Long ownerId, Pageable pageable) {
        return houseRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public List<House> latestHouses() {
        return houseRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<House> recommendedHouses() {
        return houseRepository.findTop10ByRecommendedTrueOrderByUpdatedAtDesc();
    }

    @Transactional
    public House markRecommended(Long houseId, boolean recommended) {
        House house = requireHouse(houseId);
        house.setRecommended(recommended);
        return houseRepository.save(house);
    }

    @Transactional
    public House updateStatus(Long houseId, HouseStatus status) {
        House house = requireHouse(houseId);
        house.setStatus(status);
        return houseRepository.save(house);
    }

    @Transactional(readOnly = true)
    public List<House> randomRecommended(int size) {
        List<House> candidates = houseRepository.findAll();
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        return random.ints(0, candidates.size())
                .distinct()
                .limit(Math.min(size, candidates.size()))
                .mapToObj(candidates::get)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long houseId) {
        return favoriteRepository.findByUserIdAndHouseId(userId, houseId)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    HouseFavorite favorite = new HouseFavorite();
                    favorite.setUser(userRepository.getReferenceById(userId));
                    favorite.setHouse(houseRepository.getReferenceById(houseId));
                    favoriteRepository.save(favorite);
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public List<HouseFavorite> listFavorites(Long userId) {
        return favoriteRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<HouseFavorite> listAllFavorites() {
        return favoriteRepository.findAll();
    }

    @Transactional
    public void deleteHouse(Long ownerId, Long houseId) {
        House house = requireHouse(houseId);
        if (!house.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("无权删除该房源");
        }
        if (rentalOrderRepository.existsByHouseId(houseId)) {
            throw new BadRequestException("房源存在租赁订单，无法删除");
        }
        contactRecordRepository.deleteByHouseId(houseId);
        favoriteRepository.deleteByHouseId(houseId);
        houseRepository.delete(house);
    }
}
