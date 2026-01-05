package com.example.housebackend.repository;

import com.example.housebackend.domain.location.SubwayLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubwayLineRepository extends JpaRepository<SubwayLine, Long> {

    List<SubwayLine> findByRegionId(Long regionId);
}
