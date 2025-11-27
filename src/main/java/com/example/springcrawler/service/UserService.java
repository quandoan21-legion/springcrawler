package com.example.springcrawler.service;

import com.example.springcrawler.model.User;
import com.example.springcrawler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
            throw new UsernameNotFoundException("Account has not been activated. Please verify the OTP sent to your email.");
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

    // ========== USER REGISTRATION WITH OTP ==========
    public boolean registerWithOTP(User user) {
        Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.isEnabled()) {
                return false; // already active → do not re-register
            } else {
                // not yet activated
                if (existingUser.getLastOtpSentTime() == null) {
                    // OTP has not been sent before → send now
                    sendNewOTP(existingUser);
                } else {
                    // OTP sent previously → follow resend flow
                    resendOTP(existingUser.getEmail());
                }
                return true;
            }
        }

        // Email does not exist → create a new user
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(false);
        sendNewOTP(user); // first OTP
        userRepository.save(user);

        return true;
    }

    // Send a new OTP and invalidate the previous one
    private void sendNewOTP(User user) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        user.setOtp(otp);
        user.setOtpExpiredTime(LocalDateTime.now().plusMinutes(1));
        user.setLastOtpSentTime(LocalDateTime.now());
        userRepository.save(user);
        emailService.sendOTPEmail(user.getEmail(), otp);
    }

    // ========== RESEND OTP ==========
    public String resendOTP(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return "Email does not exist.";

        User user = optionalUser.get();

        if (user.isEnabled()) return "Account has already been activated.";

        // Check the timestamp of the last OTP email
        if (user.getLastOtpSentTime() != null) {
            long secondsSinceLast = java.time.Duration.between(user.getLastOtpSentTime(), LocalDateTime.now()).getSeconds();
            if (secondsSinceLast < 60) {
                long wait = 60 - secondsSinceLast;
                return "Please wait " + wait + " seconds before requesting another OTP.";
            }
        }

        // Send new OTP and invalidate the previous one
        sendNewOTP(user);
        return "A new OTP has been sent to: " + user.getEmail();
    }

    // ========== OTP VERIFICATION ==========
    public boolean verifyOTP(String email, String otp) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return false;

        User user = optionalUser.get();

        if (user.getOtp() != null &&
                user.getOtp().equals(otp) &&
                LocalDateTime.now().isBefore(user.getOtpExpiredTime())) {

            // Valid OTP → activate the user
            user.setEnabled(true);
            user.setOtp(null);
            user.setOtpExpiredTime(null);
            userRepository.save(user);
            return true;
        }

        return false; // OTP invalid or expired
    }

    // ===== USER MANAGEMENT =====
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);
    }

    public Page<User> getUsersPage(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(trimmed, trimmed, pageable);
        }
        return userRepository.findAll(pageable);
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
    // Send OTP for password reset
    public String sendResetOTP(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if(optionalUser.isEmpty()) return "Email does not exist.";

        User user = optionalUser.get();
        sendNewOTP(user); // generate OTP and update lastOtpSentTime

        // Send email
        emailService.sendEmail(user.getEmail(), "Password reset OTP",
                "Your OTP code is: " + user.getOtp());

        return "A new OTP has been sent to your email.";
    }

    // Update password
    public void updatePassword(String email, String newPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        optionalUser.ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOtp(null); // clear OTP
            user.setLastOtpSentTime(null); // reset OTP timestamp
            userRepository.save(user);
        });
    }


}
