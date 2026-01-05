package com.example.housebackend.repository;

import com.example.housebackend.domain.order.RentalOrder;
import com.example.housebackend.domain.order.RentalOrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {

    Page<RentalOrder> findByTenantId(Long tenantId, Pageable pageable);

    Page<RentalOrder> findByLandlordId(Long landlordId, Pageable pageable);

    @Query("select r from RentalOrder r where r.house.owner.id = :ownerId")
    Page<RentalOrder> findByHouseOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    long countByStatus(RentalOrderStatus status);

    @Query("select count(r) from RentalOrder r where r.createdAt between :start and :end")
    long countByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    List<RentalOrder> findTop10ByStatusOrderByUpdatedAtDesc(RentalOrderStatus status);

    boolean existsByHouseId(Long houseId);

    @Query("""
            select case when count(r) > 0 then true else false end
            from RentalOrder r
            where r.house.id = :houseId
              and r.status in :statuses
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    boolean hasOverlappingOrders(@Param("houseId") Long houseId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("statuses") List<RentalOrderStatus> statuses);
}
