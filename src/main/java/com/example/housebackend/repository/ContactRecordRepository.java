package com.example.housebackend.repository;

import com.example.housebackend.domain.contact.ContactRecord;
import com.example.housebackend.domain.contact.ContactStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRecordRepository extends JpaRepository<ContactRecord, Long> {

    Page<ContactRecord> findByTenantId(Long tenantId, Pageable pageable);

    Page<ContactRecord> findByLandlordId(Long landlordId, Pageable pageable);

    long countByStatus(ContactStatus status);

    List<ContactRecord> findTop10ByOrderByCreatedAtDesc();

    void deleteByHouseId(Long houseId);

    Optional<ContactRecord> findFirstByTenantIdAndHouseIdOrderByCreatedAtDesc(Long tenantId, Long houseId);

    @EntityGraph(attributePaths = {"house", "tenant", "landlord"})
    Optional<ContactRecord> findWithRelationsById(Long id);
}
