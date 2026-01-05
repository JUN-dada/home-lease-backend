package com.example.housebackend.repository;

import com.example.housebackend.domain.certification.LandlordCertification;
import com.example.housebackend.domain.certification.LandlordCertificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface LandlordCertificationRepository extends JpaRepository<LandlordCertification, Long> {

    @EntityGraph(attributePaths = {"documentUrls", "user"})
    Optional<LandlordCertification> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"documentUrls", "user"})
    Page<LandlordCertification> findByStatus(LandlordCertificationStatus status, Pageable pageable);

    List<LandlordCertification> findTop10ByStatusOrderByCreatedAtDesc(LandlordCertificationStatus status);
}
