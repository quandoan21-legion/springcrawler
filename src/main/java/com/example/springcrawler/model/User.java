package com.example.springcrawler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Thời gian tạo và cập nhật
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // Tài khoản chưa active → phải xác minh OTP mới login được
    private boolean enabled = false;

    // Lưu OTP gửi qua email
    private String otp;

    // Thời điểm hết hạn OTP
    private LocalDateTime otpExpiredTime;
}
