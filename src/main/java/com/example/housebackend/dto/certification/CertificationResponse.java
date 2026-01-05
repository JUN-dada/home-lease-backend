package com.example.housebackend.dto.certification;

import com.example.housebackend.domain.certification.LandlordCertificationStatus;
import java.time.Instant;
import java.util.List;

public record CertificationResponse(Long id,
                                    LandlordCertificationStatus status,
                                    List<String> documentUrls,
                                    String reason,
                                    Instant createdAt,
                                    Instant reviewedAt,
                                    Long applicantId,
                                    String applicantName) {
}
