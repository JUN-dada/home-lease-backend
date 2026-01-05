package com.example.housebackend.service;

import com.example.housebackend.domain.contact.ContactRecord;
import com.example.housebackend.domain.contact.ContactStatus;
import com.example.housebackend.domain.house.House;
import com.example.housebackend.exception.AccessDeniedException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.ContactRecordRepository;
import com.example.housebackend.repository.HouseRepository;
import com.example.housebackend.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRecordRepository contactRecordRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;

    @Transactional
    public ContactRecord ensureConversation(Long tenantId, Long houseId, String message, LocalDateTime visitTime) {
        return contactRecordRepository.findFirstByTenantIdAndHouseIdOrderByCreatedAtDesc(tenantId, houseId)
                .orElseGet(() -> createRecord(tenantId, houseId, message, visitTime));
    }

    @Transactional
    public ContactRecord createRecord(Long tenantId, Long houseId, String message, LocalDateTime visitTime) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("房源不存在"));
        ContactRecord record = new ContactRecord();
        record.setHouse(house);
        record.setTenant(userRepository.getReferenceById(tenantId));
        record.setLandlord(house.getOwner());
        record.setMessage(message);
        record.setPreferredVisitTime(visitTime);
        record.setStatus(ContactStatus.PENDING);
        return contactRecordRepository.save(record);
    }

    @Transactional
    public ContactRecord updateStatus(Long landlordId, Long recordId, ContactStatus status, String remarks) {
        ContactRecord record = contactRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("联系记录不存在"));
        if (!record.getLandlord().getId().equals(landlordId)) {
            throw new AccessDeniedException("无权处理该联系记录");
        }
        record.setStatus(status);
        record.setRemarks(remarks);
        record.setHandledAt(Instant.now());
        return contactRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Page<ContactRecord> listForTenant(Long tenantId, Pageable pageable) {
        return contactRecordRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ContactRecord> listForLandlord(Long landlordId, Pageable pageable) {
        return contactRecordRepository.findByLandlordId(landlordId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ContactRecord> listForAdmin(Pageable pageable) {
        return contactRecordRepository.findAll(pageable);
    }
}
