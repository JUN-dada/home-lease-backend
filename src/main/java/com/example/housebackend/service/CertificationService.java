package com.example.housebackend.service;

import com.example.housebackend.domain.certification.LandlordCertification;
import com.example.housebackend.domain.certification.LandlordCertificationStatus;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.LandlordCertificationRepository;
import com.example.housebackend.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.List;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final LandlordCertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public LandlordCertification submitCertification(Long userId, List<String> documentUrls, String reason) {
        List<String> sanitized = documentUrls == null ? List.of() : documentUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (sanitized.isEmpty()) {
            throw new BadRequestException("请至少上传一份认证资料");
        }
        LandlordCertification certification = new LandlordCertification();
        certification.setUser(userRepository.getReferenceById(userId));
        certification.setDocumentUrl(sanitized.get(0));
        certification.setDocumentUrls(new java.util.ArrayList<>(sanitized));
        certification.setReason(reason);
        certification.setStatus(LandlordCertificationStatus.PENDING);
        LandlordCertification saved = certificationRepository.save(certification);
        notifyAdmin(saved);
        return saved;
    }

    @Transactional
    public LandlordCertification reviewCertification(Long adminId,
                                                     Long certificationId,
                                                     LandlordCertificationStatus status,
                                                     String reason) {
        LandlordCertification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("认证申请不存在"));
        certification.setStatus(status);
        certification.setReason(reason);
        certification.setReviewedBy(userRepository.getReferenceById(adminId));
        certification.setReviewedAt(java.time.Instant.now());
        if (status == LandlordCertificationStatus.APPROVED) {
            var user = certification.getUser();
            user.setRole(com.example.housebackend.domain.user.UserRole.LANDLORD);
            userRepository.save(user);
        }
        return certificationRepository.save(certification);
    }

    @Transactional(readOnly = true)
    public LandlordCertification latestForUser(Long userId) {
        return certificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isCertifiedLandlord(Long userId) {
        return certificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(certification -> certification.getStatus() == LandlordCertificationStatus.APPROVED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<LandlordCertification> listByStatus(LandlordCertificationStatus status, Pageable pageable) {
        return certificationRepository.findByStatus(status, pageable);
    }

    private void notifyAdmin(LandlordCertification certification) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/admin/certifications",
                    com.example.housebackend.dto.DtoMapper.toCertification(certification));
        }
    }
}
