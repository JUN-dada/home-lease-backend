package com.example.housebackend.repository;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByRole(UserRole role);

    boolean existsByUsername(String username);
}
