package com.example.housebackend.dto.certification;

import com.example.housebackend.domain.certification.LandlordCertificationStatus;

public record CertificationReviewRequest(LandlordCertificationStatus status, String reason) {
}
