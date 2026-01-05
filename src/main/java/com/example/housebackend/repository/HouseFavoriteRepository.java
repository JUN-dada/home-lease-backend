package com.example.housebackend.repository;

import com.example.housebackend.domain.house.HouseFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseFavoriteRepository extends JpaRepository<HouseFavorite, Long> {

    List<HouseFavorite> findByUserId(Long userId);

    Optional<HouseFavorite> findByUserIdAndHouseId(Long userId, Long houseId);

    long countByHouseId(Long houseId);

    void deleteByHouseId(Long houseId);
}
