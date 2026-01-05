package com.example.housebackend.service;

import com.example.housebackend.domain.auth.AuthToken;
import com.example.housebackend.domain.user.User;
import com.example.housebackend.domain.user.UserRole;
import com.example.housebackend.exception.BadRequestException;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.AuthTokenRepository;
import com.example.housebackend.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String password, String fullName, String phone, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public AuthToken login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("密码错误");
        }
        user.setLastLoginAt(Instant.now());
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return authTokenRepository.save(token);
    }

    @Transactional
    public void logout(String tokenValue) {
        authTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    authTokenRepository.save(token);
                });
    }

    @Transactional(readOnly = true)
    public User requireUser(String tokenValue) {
        AuthToken token = authTokenRepository.findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new BadRequestException("无效的访问令牌"));
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("访问令牌已过期");
        }
        return token.getUser();
    }
}
