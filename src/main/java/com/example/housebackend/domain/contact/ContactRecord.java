package com.example.housebackend.domain.contact;

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
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "contact_records")
public class ContactRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id")
    private House house;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id")
    private User landlord;

    @Column(length = 512)
    private String message;

    private LocalDateTime preferredVisitTime;

    private Instant handledAt;

    @Enumerated(EnumType.STRING)
    private ContactStatus status = ContactStatus.PENDING;

    @Column(length = 256)
    private String remarks;
}
