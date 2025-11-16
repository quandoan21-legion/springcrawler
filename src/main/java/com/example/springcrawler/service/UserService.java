package com.example.springcrawler.service;

import com.example.springcrawler.model.User;
import com.example.springcrawler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP trong email.");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles("ADMIN")
                .build();
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ========== ĐĂNG KÝ NGƯỜI DÙNG VỚI OTP ==========
    public boolean registerWithOTP(User user) {
        Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.isEnabled()) {
                return false; // đã active → không đăng ký lại
            } else {
                // chưa active
                if (existingUser.getLastOtpSentTime() == null) {
                    // Lần đầu chưa gửi OTP → gửi luôn
                    sendNewOTP(existingUser);
                } else {
                    // Đã gửi OTP trước đó → giữ luồng resend
                    resendOTP(existingUser.getEmail());
                }
                return true;
            }
        }

        // Email chưa tồn tại → tạo mới
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(false);
        sendNewOTP(user); // gửi OTP lần đầu
        userRepository.save(user);

        return true;
    }

    // Gửi OTP mới, invalidate OTP cũ
    private void sendNewOTP(User user) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        user.setOtp(otp);
        user.setOtpExpiredTime(LocalDateTime.now().plusMinutes(1));
        user.setLastOtpSentTime(LocalDateTime.now());
        userRepository.save(user);
        emailService.sendOTPEmail(user.getEmail(), otp);
    }

    // ========== GỬI LẠI OTP ==========
    public String resendOTP(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return "Email không tồn tại!";

        User user = optionalUser.get();

        if (user.isEnabled()) return "Tài khoản đã được kích hoạt!";

        // Kiểm tra thời gian gửi OTP gần nhất
        if (user.getLastOtpSentTime() != null) {
            long secondsSinceLast = java.time.Duration.between(user.getLastOtpSentTime(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < 60) {
                long wait = 60 - secondsSinceLast;
                return "Bạn phải chờ " + wait + " giây trước khi gửi lại OTP!";
            }
        }

        // Gửi OTP mới → OTP cũ hết hiệu lực
        sendNewOTP(user);
        return "OTP mới đã được gửi tới email: " + user.getEmail();
    }

    // ========== XÁC THỰC OTP ==========
    public boolean verifyOTP(String email, String otp) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return false;

        User user = optionalUser.get();

        if (user.getOtp() != null &&
                user.getOtp().equals(otp) &&
                LocalDateTime.now().isBefore(user.getOtpExpiredTime())) {

            // OTP hợp lệ → kích hoạt user
            user.setEnabled(true);
            user.setOtp(null);
            user.setOtpExpiredTime(null);
            userRepository.save(user);
            return true;
        }

        return false; // OTP sai hoặc hết hạn
    }

    // ===== QUẢN LÝ NGƯỜI DÙNG =====
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
    // Gửi OTP để reset password
    public String sendResetOTP(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if(optionalUser.isEmpty()) return "Email không tồn tại!";

        User user = optionalUser.get();
        sendNewOTP(user); // generate OTP, lưu lastOtpSentTime

        // Gửi email
        emailService.sendEmail(user.getEmail(), "OTP đặt lại mật khẩu",
                "Mã OTP của bạn là: " + user.getOtp());

        return "OTP mới đã được gửi tới email của bạn!";
    }

    // Cập nhật mật khẩu mới
    public void updatePassword(String email, String newPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        optionalUser.ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOtp(null); // xóa OTP
            user.setLastOtpSentTime(null); // reset thời gian gửi OTP
            userRepository.save(user);
        });
    }


}
