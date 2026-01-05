package com.example.housebackend.service;

import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    @Transactional
    public User updateProfile(Long userId, User updated) {
        User user = getProfile(userId);
        user.setFullName(updated.getFullName());
        user.setAvatarUrl(updated.getAvatarUrl());
        user.setEmail(updated.getEmail());
        user.setPhone(updated.getPhone());
        user.setGender(updated.getGender());
        user.setBio(updated.getBio());
        user.setIdNumber(updated.getIdNumber());
        user.replaceMedia(updated.getMedia());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getProfile(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> listByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getProfile(userId);
        userRepository.delete(user);
    }
}
