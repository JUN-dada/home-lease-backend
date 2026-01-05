package com.example.housebackend.service;

import com.example.housebackend.domain.house.House;
import com.example.housebackend.domain.order.OrderTerminationStatus;
import com.example.housebackend.domain.order.RentalOrder;
import com.example.housebackend.domain.order.RentalOrderStatus;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.exception.AccessDeniedException;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.HouseRepository;
import com.example.housebackend.repository.RentalOrderRepository;
import com.example.housebackend.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalOrderService {

    private final RentalOrderRepository rentalOrderRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;

    @Transactional
    public RentalOrder createOrder(Long tenantId,
                                   Long houseId,
                                   LocalDate startDate,
                                   LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("请提供完整的租期");
        }
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        if (house.getOwner() == null) {
            throw new BadRequestException("房源无房东信息");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("租赁结束时间不能早于开始时间");
        }
        boolean hasOverlap = rentalOrderRepository.hasOverlappingOrders(
                houseId,
                startDate,
                endDate,
                List.of(RentalOrderStatus.CONFIRMED, RentalOrderStatus.ACTIVE));
        if (hasOverlap) {
            throw new BadRequestException("该房源在所选日期已被预订");
        }
        RentalOrder order = new RentalOrder();
        order.setHouse(house);
        order.setTenant(userRepository.getReferenceById(tenantId));
        order.setLandlord(house.getOwner());
        order.setStartDate(startDate);
        order.setEndDate(endDate);
        order.setMonthlyRent(house.getRentPrice());
        order.setDeposit(house.getDeposit() != null ? house.getDeposit() : house.getRentPrice());
        order.setStatus(RentalOrderStatus.PENDING);
        return rentalOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public RentalOrder getOrder(Long orderId) {
        return rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
    }

    @Transactional(readOnly = true)
    public Page<RentalOrder> listForTenant(Long tenantId, Pageable pageable) {
        return rentalOrderRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<RentalOrder> listForLandlord(Long landlordId, Pageable pageable) {
        return rentalOrderRepository.findByLandlordId(landlordId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<RentalOrder> listForAdmin(Pageable pageable) {
        return rentalOrderRepository.findAll(pageable);
    }

    @Transactional
    public RentalOrder cancelOrder(Long tenantId, Long orderId) {
        RentalOrder order = getOrder(orderId);
        if (!order.getTenant().getId().equals(tenantId)) {
            throw new AccessDeniedException("无权取消该订单");
        }
        if (order.getStatus() == RentalOrderStatus.CONFIRMED || order.getStatus() == RentalOrderStatus.ACTIVE) {
            throw new BadRequestException("订单已确认，无法取消");
        }
        order.setStatus(RentalOrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder confirmOrder(Long landlordId, Long orderId) {
        RentalOrder order = getOrder(orderId);
        requireLandlordAccess(landlordId, order);
        order.setStatus(RentalOrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder uploadContract(Long landlordId, Long orderId, String contractUrl) {
        RentalOrder order = getOrder(orderId);
        requireLandlordAccess(landlordId, order);
        order.setContractUrl(contractUrl);
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder activateOrder(Long landlordId, Long orderId) {
        RentalOrder order = getOrder(orderId);
        requireLandlordAccess(landlordId, order);
        order.setStatus(RentalOrderStatus.ACTIVE);
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder requestTermination(User requester, Long orderId, String reason) {
        RentalOrder order = getOrder(orderId);
        ensureParticipant(requester, order);
        if (order.getStatus() == RentalOrderStatus.CANCELLED || order.getStatus() == RentalOrderStatus.TERMINATED) {
            throw new BadRequestException("订单已结束，无法发起终止申请");
        }
        if (order.getTerminationStatus() == OrderTerminationStatus.REQUESTED) {
            throw new BadRequestException("已存在待处理的终止申请");
        }
        order.setTerminationStatus(OrderTerminationStatus.REQUESTED);
        order.setTerminationRequester(requester);
        order.setTerminationRequestedAt(Instant.now());
        order.setTerminationReason(reason);
        order.setTerminationResolver(null);
        order.setTerminationResolvedAt(null);
        order.setTerminationFeedback(null);
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder approveTermination(User resolver, Long orderId, String feedback) {
        RentalOrder order = getOrder(orderId);
        ensureCanResolveTermination(resolver, order);
        if (order.getTerminationStatus() != OrderTerminationStatus.REQUESTED) {
            throw new BadRequestException("当前无待处理的终止申请");
        }
        order.setTerminationStatus(OrderTerminationStatus.APPROVED);
        order.setTerminationResolver(resolver);
        order.setTerminationFeedback(feedback);
        order.setTerminationResolvedAt(Instant.now());
        order.setStatus(RentalOrderStatus.TERMINATED);
        return rentalOrderRepository.save(order);
    }

    @Transactional
    public RentalOrder rejectTermination(User resolver, Long orderId, String feedback) {
        RentalOrder order = getOrder(orderId);
        ensureCanResolveTermination(resolver, order);
        if (order.getTerminationStatus() != OrderTerminationStatus.REQUESTED) {
            throw new BadRequestException("当前无待处理的终止申请");
        }
        order.setTerminationStatus(OrderTerminationStatus.REJECTED);
        order.setTerminationResolver(resolver);
        order.setTerminationFeedback(feedback);
        order.setTerminationResolvedAt(Instant.now());
        return rentalOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public String downloadContract(User requester, Long orderId) {
        RentalOrder order = getOrder(orderId);
        if (!order.getTenant().getId().equals(requester.getId())
                && !order.getLandlord().getId().equals(requester.getId())
                && requester.getRole() != com.example.housebackend.domain.user.UserRole.ADMIN) {
            throw new AccessDeniedException("无权下载合同");
        }
        return order.getContractUrl();
    }

    private void requireLandlordAccess(Long landlordId, RentalOrder order) {
        if (!order.getLandlord().getId().equals(landlordId)) {
            throw new AccessDeniedException("无权操作该订单");
        }
    }

    private void ensureParticipant(User user, RentalOrder order) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        Long userId = user.getId();
        if (!order.getTenant().getId().equals(userId) && !order.getLandlord().getId().equals(userId)) {
            throw new AccessDeniedException("无权操作该订单");
        }
    }

    private void ensureCanResolveTermination(User resolver, RentalOrder order) {
        if (resolver.getRole() == UserRole.ADMIN) {
            return;
        }
        ensureParticipant(resolver, order);
        if (order.getTerminationRequester() != null
                && order.getTerminationRequester().getId().equals(resolver.getId())) {
            throw new AccessDeniedException("申请人无法审核自己的终止请求");
        }
    }
}
