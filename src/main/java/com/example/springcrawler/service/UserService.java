package com.example.springcrawler.service;

import com.example.springcrawler.model.User;
import com.example.springcrawler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
         // Đăng ký user
    public String register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email đã tồn tại!";
        }
         // Mã hóa password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "Đăng ký thành công!";
    }
        // Đăng nhập user
    public String login(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                        return "Đăng nhập thành công!";
                    } else {
                        return "Mật khẩu không đúng!";
                    }
                })
                .orElse("Email không tồn tại!");
    }
}