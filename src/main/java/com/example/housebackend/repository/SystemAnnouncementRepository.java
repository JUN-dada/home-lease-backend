package com.example.housebackend.repository;

import com.example.housebackend.domain.announcement.SystemAnnouncement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemAnnouncementRepository extends JpaRepository<SystemAnnouncement, Long> {

    List<SystemAnnouncement> findTop5ByOrderByCreatedAtDesc();

    List<SystemAnnouncement> findByPinnedTrueOrderByCreatedAtDesc();
}
