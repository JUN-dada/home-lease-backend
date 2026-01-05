package com.example.housebackend.domain.order;

import com.example.housebackend.domain.common.BaseEntity;
import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rental_orders")
public class RentalOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id")
    private User landlord;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal monthlyRent;

    private BigDecimal deposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RentalOrderStatus status = RentalOrderStatus.PENDING;

    @Column(length = 256)
    private String contractUrl;

    private Instant cancelledAt;

    private Instant confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderTerminationStatus terminationStatus = OrderTerminationStatus.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_requester_id")
    private User terminationRequester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termination_resolver_id")
    private User terminationResolver;

    private Instant terminationRequestedAt;

    private Instant terminationResolvedAt;

    @Column(length = 512)
    private String terminationReason;

    @Column(length = 512)
    private String terminationFeedback;
}
