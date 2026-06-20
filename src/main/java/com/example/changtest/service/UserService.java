package com.example.changtest.service;

import com.example.changtest.entity.User;
import com.example.changtest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void register(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("用户名长度不能超过50个字符");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6个字符");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return user;
        }
        return null;
    }
}
