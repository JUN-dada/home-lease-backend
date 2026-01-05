package com.example.housebackend.domain.certification;

import com.example.housebackend.domain.common.BaseEntity;
import com.example.housebackend.domain.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "landlord_certifications")
public class LandlordCertification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 256)
    private String documentUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "landlord_certification_documents", joinColumns = @JoinColumn(name = "certification_id"))
    @Column(name = "document_url", length = 512)
    private List<String> documentUrls = new ArrayList<>();

    @Column(length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    private LandlordCertificationStatus status = LandlordCertificationStatus.PENDING;

    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
}
