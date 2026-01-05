package com.example.housebackend.repository;

import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.house.HouseStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HouseRepository extends JpaRepository<House, Long> {

    Page<House> findByOwnerId(Long ownerId, Pageable pageable);

    Page<House> findByStatus(HouseStatus status, Pageable pageable);

    List<House> findTop10ByRecommendedTrueOrderByUpdatedAtDesc();

    List<House> findTop10ByOrderByCreatedAtDesc();

    @Query("select h from House h where (:regionId is null or h.region.id = :regionId) "
            + "and (:subwayId is null or h.subwayLine.id = :subwayId) "
            + "and (:status is null or h.status = :status)")
    Page<House> search(@org.springframework.data.repository.query.Param("regionId") Long regionId,
                       @org.springframework.data.repository.query.Param("subwayId") Long subwayId,
                       @org.springframework.data.repository.query.Param("status") HouseStatus status,
                       Pageable pageable);
}
