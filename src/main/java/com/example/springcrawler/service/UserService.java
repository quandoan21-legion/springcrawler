package com.example.springcrawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.t2404e.spring_prj.model.User;
import org.t2404e.spring_prj.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ========== UserDetailsService cho Spring Security ==========
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Chỉ cho login nếu đã active
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP trong email.");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles("ADMIN") // Thay bằng role thực tế nếu cần
                .build();
    }

    // ========== ĐĂNG KÝ NGƯỜI DÙNG (không dùng OTP, giữ nguyên nếu muốn) ==========
    public String register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email đã tồn tại!";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "Đăng ký thành công!";
    }

    // ========== ĐĂNG KÝ NGƯỜI DÙNG VỚI OTP ==========
    public String registerWithOTP(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email đã tồn tại!";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Sinh OTP 6 chữ số
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        user.setOtp(otp);
        user.setOtpExpiredTime(LocalDateTime.now().plusMinutes(5));
        user.setEnabled(false); // chưa active

        userRepository.save(user);

        // Gửi OTP qua email
        emailService.sendOTPEmail(user.getEmail(), otp);

        return "Đăng ký thành công! OTP đã được gửi tới email: " + user.getEmail();
    }

    // ========== XÁC THỰC OTP ==========
    public boolean verifyOTP(String email, String otp) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return false;

        User user = optionalUser.get();
        if (user.getOtp() != null &&
                user.getOtp().equals(otp) &&
                LocalDateTime.now().isBefore(user.getOtpExpiredTime())) {

            // OTP hợp lệ → kích hoạt tài khoản
            user.setEnabled(true);
            user.setOtp(null);
            user.setOtpExpiredTime(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // ========== QUẢN LÝ NGƯỜI DÙNG ==========
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void updateUser(Long id, String fullName, String email, String password) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setFullName(fullName);
            user.setEmail(email);
            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
            userRepository.save(user);
        }
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
