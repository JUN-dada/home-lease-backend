package com.example.housebackend.repository;

import com.example.housebackend.domain.house.HouseMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseMediaRepository extends JpaRepository<HouseMedia, Long> {

    List<HouseMedia> findByHouseId(Long houseId);
}
