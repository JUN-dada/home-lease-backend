package com.example.housebackend.service;

import com.example.housebackend.domain.announcement.SystemAnnouncement;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.SystemAnnouncementRepository;
import com.example.housebackend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final SystemAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    @Transactional
    public SystemAnnouncement create(Long adminId, SystemAnnouncement announcement) {
        announcement.setCreatedBy(userRepository.getReferenceById(adminId));
        return announcementRepository.save(announcement);
    }

    @Transactional
    public SystemAnnouncement update(Long adminId, Long announcementId, SystemAnnouncement incoming) {
        SystemAnnouncement announcement = requireAnnouncement(announcementId);
        announcement.setTitle(incoming.getTitle());
        announcement.setContent(incoming.getContent());
        announcement.setPinned(incoming.isPinned());
        announcement.setCreatedBy(userRepository.getReferenceById(adminId));
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void delete(Long announcementId) {
        announcementRepository.deleteById(announcementId);
    }

    @Transactional(readOnly = true)
    public SystemAnnouncement requireAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("公告不存在"));
    }

    @Transactional(readOnly = true)
    public Page<SystemAnnouncement> list(Pageable pageable) {
        return announcementRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<SystemAnnouncement> latest() {
        return announcementRepository.findTop5ByOrderByCreatedAtDesc();
    }
}
