package com.example.housebackend.controller;

import com.example.housebackend.domain.certification.LandlordCertificationStatus;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.certification.CertificationResponse;
import com.example.housebackend.dto.certification.CertificationReviewRequest;
import com.example.housebackend.dto.certification.CertificationSubmitRequest;
import com.example.housebackend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.example.housebackend.service.CertificationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
@Tag(name = "房东认证", description = "完成房东认证申请与审核的相关流程")
public class CertificationController {

    private final AuthService authService;
    private final CertificationService certificationService;

    @PostMapping
    @Operation(summary = "提交房东认证申请", description = "租客提交房东认证所需的资料")
    public ResponseEntity<CertificationResponse> submit(@RequestHeader("X-Auth-Token") String token,
                                                        @RequestBody CertificationSubmitRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER, UserRole.LANDLORD);
        return ResponseEntity.ok(DtoMapper.toCertification(certificationService.submitCertification(
                user.getId(),
                request.documentUrls(),
                request.reason())));
    }

    @GetMapping("/me")
    @Operation(summary = "查看个人认证", description = "租客查询自己最近一次的房东认证结果")
    public ResponseEntity<CertificationResponse> myCertification(@RequestHeader("X-Auth-Token") String token) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER, UserRole.LANDLORD);
        return Optional.ofNullable(certificationService.latestForUser(user.getId()))
                .map(DtoMapper::toCertification)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping
    @Operation(summary = "分页查询认证申请", description = "管理员按状态分页查看房东认证申请")
    public ResponseEntity<Page<CertificationResponse>> list(@RequestHeader("X-Auth-Token") String token,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size,
                                                            @RequestParam(required = false) LandlordCertificationStatus status) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        LandlordCertificationStatus queryStatus = status != null ? status : LandlordCertificationStatus.PENDING;
        Page<CertificationResponse> responses = certificationService.listByStatus(queryStatus, PageRequest.of(page, size))
                .map(DtoMapper::toCertification);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{certificationId}/review")
    @Operation(summary = "审核认证申请", description = "管理员审核指定的房东认证申请")
    public ResponseEntity<CertificationResponse> review(@RequestHeader("X-Auth-Token") String token,
                                                        @PathVariable Long certificationId,
                                                        @RequestBody CertificationReviewRequest request) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        return ResponseEntity.ok(DtoMapper.toCertification(certificationService.reviewCertification(
                admin.getId(), certificationId, request.status(), request.reason())));
    }

    private void ensureRole(User user, UserRole... roles) {
        for (UserRole role : roles) {
            if (user.getRole() == role) {
                return;
            }
        }
        throw new com.example.housebackend.exception.AccessDeniedException("无权访问该资源");
    }
}
