package com.example.housebackend.repository;

import com.example.housebackend.domain.user.UserMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMediaRepository extends JpaRepository<UserMedia, Long> {

    List<UserMedia> findByUserId(Long userId);
}
