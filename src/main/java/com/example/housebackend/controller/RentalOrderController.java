package com.example.housebackend.controller;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.dto.DtoMapper;
import com.example.housebackend.dto.order.ContractUploadRequest;
import com.example.housebackend.dto.order.RentalOrderCreateRequest;
import com.example.housebackend.dto.order.RentalOrderResponse;
import com.example.housebackend.dto.order.TerminateOrderRequest;
import com.example.housebackend.dto.order.TerminationDecisionRequest;
import com.example.housebackend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.example.housebackend.service.RentalOrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "租赁订单", description = "涵盖租赁订单的创建、管理与合同流程")
public class RentalOrderController {

    private final AuthService authService;
    private final RentalOrderService rentalOrderService;

    @PostMapping
    @Operation(summary = "创建租赁订单", description = "租客提交租赁需求，生成租赁订单")
    public ResponseEntity<RentalOrderResponse> createOrder(@RequestHeader("X-Auth-Token") String token,
                                                           @RequestBody RentalOrderCreateRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER);
        return ResponseEntity.ok(DtoMapper.toOrder(rentalOrderService.createOrder(
                user.getId(),
                request.houseId(),
                request.startDate(),
                request.endDate())));
    }

    @GetMapping("/mine")
    @Operation(summary = "租客订单列表", description = "分页查询当前租客的租赁订单")
    public ResponseEntity<Page<RentalOrderResponse>> myOrders(@RequestHeader("X-Auth-Token") String token,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.USER);
        Page<RentalOrderResponse> responses = rentalOrderService.listForTenant(user.getId(), PageRequest.of(page, size))
                .map(DtoMapper::toOrder);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/landlord")
    @Operation(summary = "房东订单列表", description = "分页查询当前房东的租赁订单")
    public ResponseEntity<Page<RentalOrderResponse>> landlordOrders(@RequestHeader("X-Auth-Token") String token,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        Page<RentalOrderResponse> responses = rentalOrderService.listForLandlord(landlord.getId(), PageRequest.of(page, size))
                .map(DtoMapper::toOrder);
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @Operation(summary = "管理员订单列表", description = "分页查询平台全部租赁订单")
    public ResponseEntity<Page<RentalOrderResponse>> allOrders(@RequestHeader("X-Auth-Token") String token,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        User admin = authService.requireUser(token);
        ensureRole(admin, UserRole.ADMIN);
        Page<RentalOrderResponse> responses = rentalOrderService.listForAdmin(PageRequest.of(page, size))
                .map(DtoMapper::toOrder);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "租客取消订单", description = "租客根据订单 ID 取消待处理订单")
    public ResponseEntity<RentalOrderResponse> cancelOrder(@RequestHeader("X-Auth-Token") String token,
                                                           @PathVariable Long orderId) {
        User tenant = authService.requireUser(token);
        ensureRole(tenant, UserRole.USER);
        return ResponseEntity.ok(DtoMapper.toOrder(rentalOrderService.cancelOrder(tenant.getId(), orderId)));
    }

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "房东确认订单", description = "房东确认租客提交的订单")
    public ResponseEntity<RentalOrderResponse> confirmOrder(@RequestHeader("X-Auth-Token") String token,
                                                            @PathVariable Long orderId) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        return ResponseEntity.ok(DtoMapper.toOrder(rentalOrderService.confirmOrder(landlord.getId(), orderId)));
    }

    @PostMapping("/{orderId}/activate")
    @Operation(summary = "房东激活订单", description = "房东在租约生效时激活订单")
    public ResponseEntity<RentalOrderResponse> activateOrder(@RequestHeader("X-Auth-Token") String token,
                                                             @PathVariable Long orderId) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        return ResponseEntity.ok(DtoMapper.toOrder(rentalOrderService.activateOrder(landlord.getId(), orderId)));
    }

    @PostMapping("/{orderId}/contract")
    @Operation(summary = "上传合同", description = "房东为订单上传电子合同地址")
    public ResponseEntity<RentalOrderResponse> uploadContract(@RequestHeader("X-Auth-Token") String token,
                                                              @PathVariable Long orderId,
                                                              @RequestBody ContractUploadRequest request) {
        User landlord = authService.requireUser(token);
        ensureRole(landlord, UserRole.LANDLORD);
        return ResponseEntity.ok(DtoMapper.toOrder(rentalOrderService.uploadContract(
                landlord.getId(), orderId, request.contractUrl())));
    }

    @GetMapping("/{orderId}/contract")
    @Operation(summary = "下载合同", description = "租客或房东获取订单对应的合同链接")
    public ResponseEntity<String> downloadContract(@RequestHeader("X-Auth-Token") String token,
                                                   @PathVariable Long orderId) {
        User user = authService.requireUser(token);
        return ResponseEntity.ok(rentalOrderService.downloadContract(user, orderId));
    }

    @PostMapping("/{orderId}/terminate")
    @Operation(summary = "终止订单", description = "用户提交订单终止申请")
    public ResponseEntity<RentalOrderResponse> terminate(@RequestHeader("X-Auth-Token") String token,
                                                         @PathVariable Long orderId,
                                                         @RequestBody(required = false) TerminateOrderRequest request) {
        User user = authService.requireUser(token);
        return ResponseEntity.ok(DtoMapper.toOrder(
                rentalOrderService.requestTermination(user, orderId, request != null ? request.reason() : null)));
    }

    @PostMapping("/{orderId}/termination/approve")
    @Operation(summary = "处理终止申请", description = "房东或管理员同意终止申请")
    public ResponseEntity<RentalOrderResponse> approveTermination(@RequestHeader("X-Auth-Token") String token,
                                                                  @PathVariable Long orderId,
                                                                  @RequestBody(required = false) TerminationDecisionRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.LANDLORD, UserRole.ADMIN);
        return ResponseEntity.ok(DtoMapper.toOrder(
                rentalOrderService.approveTermination(user, orderId, request != null ? request.feedback() : null)));
    }

    @PostMapping("/{orderId}/termination/reject")
    @Operation(summary = "驳回终止申请", description = "房东或管理员驳回终止申请")
    public ResponseEntity<RentalOrderResponse> rejectTermination(@RequestHeader("X-Auth-Token") String token,
                                                                 @PathVariable Long orderId,
                                                                 @RequestBody(required = false) TerminationDecisionRequest request) {
        User user = authService.requireUser(token);
        ensureRole(user, UserRole.LANDLORD, UserRole.ADMIN);
        return ResponseEntity.ok(DtoMapper.toOrder(
                rentalOrderService.rejectTermination(user, orderId, request != null ? request.feedback() : null)));
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
