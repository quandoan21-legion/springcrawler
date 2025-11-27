package com.example.springcrawler.service.impl;

import com.example.springcrawler.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Unable to send email", e);
        }
    }

    @Override
    public void sendOTPEmail(String toEmail, String otp) {
        String subject = "Your verification OTP code";
        String content = "<h3>Your OTP code is:</h3><h2>" + otp + "</h2>";
        sendEmail(toEmail, subject, content);
    }
}
