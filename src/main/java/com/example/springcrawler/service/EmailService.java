package com.example.springcrawler.service;


public interface    EmailService {
    void sendOTPEmail(String toEmail, String otp);
    void sendEmail(String to, String subject, String content);
}
