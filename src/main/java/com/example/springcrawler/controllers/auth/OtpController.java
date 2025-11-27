package com.example.springcrawler.controllers.auth;

import com.example.springcrawler.model.User;
import com.example.springcrawler.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/api/v1/auth")
public class OtpController {

    private final UserService userService;

    public OtpController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("secondsRemaining", secondsRemaining(email));
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtpSubmit(@RequestParam String email,
                                  @RequestParam String otp,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        boolean success = userService.verifyOTP(email, otp);
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Registration successful! You can sign in now.");
            return "redirect:/api/v1/auth";
        } else {
            model.addAttribute("secondsRemaining", secondsRemaining(email));
            model.addAttribute("email", email);
            model.addAttribute("message", "OTP is invalid or has expired.");
            return "verify-otp";
        }
    }

    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam String email,
                            RedirectAttributes redirectAttributes) {
        String message = userService.resendOTP(email);
        if (message.startsWith("A new OTP")) {
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            redirectAttributes.addFlashAttribute("message", message);
        }
        redirectAttributes.addAttribute("email", email);
        return "redirect:/api/v1/auth/verify-otp";
    }

    private long secondsRemaining(String email) {
        Optional<User> optionalUser = userService.getUserByEmail(email);
        if (optionalUser.isEmpty() || optionalUser.get().getLastOtpSentTime() == null) {
            return 0;
        }
        LocalDateTime lastSent = optionalUser.get().getLastOtpSentTime();
        long secondsSinceLast = Duration.between(lastSent, LocalDateTime.now()).getSeconds();
        return Math.max(60 - secondsSinceLast, 0);
    }
}
