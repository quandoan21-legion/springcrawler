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
public class PasswordResetController {

    private final UserService userService;

    public PasswordResetController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String email, RedirectAttributes redirectAttributes) {
        Optional<User> optionalUser = userService.getUserByEmail(email);
        if (optionalUser.isPresent()) {
            String msg = userService.sendResetOTP(email);
            redirectAttributes.addFlashAttribute("success", msg);
            redirectAttributes.addAttribute("email", email);
            return "redirect:/api/v1/auth/reset-password";
        } else {
            redirectAttributes.addFlashAttribute("message", "Email does not exist.");
            return "redirect:/api/v1/auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("secondsRemaining", secondsRemaining(email));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String email,
                                      @RequestParam String otp,
                                      @RequestParam String newPassword,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {

        boolean otpValid = userService.verifyOTP(email, otp);
        if (otpValid) {
            userService.updatePassword(email, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password reset successfully! You can sign in now.");
            return "redirect:/api/v1/auth";
        } else {
            model.addAttribute("secondsRemaining", secondsRemaining(email));
            model.addAttribute("email", email);
            model.addAttribute("message", "OTP is invalid or has expired.");
            return "reset-password";
        }
    }

    @GetMapping("/resend-reset-otp")
    public String resendResetOtp(@RequestParam String email, RedirectAttributes redirectAttributes) {
        String message = userService.sendResetOTP(email);
        if (message.startsWith("A new OTP")) {
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            redirectAttributes.addFlashAttribute("message", message);
        }
        redirectAttributes.addAttribute("email", email);
        return "redirect:/api/v1/auth/reset-password";
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
