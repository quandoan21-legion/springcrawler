package com.example.springcrawler.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.springcrawler.dto.LoginRequest;
import com.example.springcrawler.model.User;
import com.example.springcrawler.service.UserService;

@Controller
@RequestMapping("api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    // Constructor injection để @Autowired AuthenticationManager + UserService
    @Autowired
    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    // Hiển thị trang login
    @GetMapping
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login"; // login.html
    }

    // Xử lý đăng nhập
    @PostMapping("/login")
    public String loginSubmit(@ModelAttribute LoginRequest loginRequest, Model model) {
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return "redirect:/admin";
        } catch (Exception e) {
            // Ghi log thay vì printStackTrace
            logger.error("Login failed for email {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            model.addAttribute("message", "Email hoặc mật khẩu không đúng!");
            model.addAttribute("loginRequest", loginRequest);
            return "login";
        }


    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register"; // register.html
    }
    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute User user, Model model) {
        String message = userService.registerWithOTP(user); // gọi method mới
        model.addAttribute("message", message);
        model.addAttribute("user", user);

        // chuyển sang trang verify OTP
        return "redirect:/api/v1/auth/verify-otp?email=" + user.getEmail();
    }
    // Hiển thị form nhập OTP
    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp"; // verify-otp.html
    }

    // Xử lý submit OTP
    @PostMapping("/verify-otp")
    public String verifyOtpSubmit(@RequestParam String email,
                                  @RequestParam String otp,
                                  Model model) {
        boolean success = userService.verifyOTP(email, otp);
        if (success) {
            return "redirect:/api/v1/auth?success=thành công!";
        } else {
            model.addAttribute("email", email);
            model.addAttribute("message", "OTP không hợp lệ hoặc đã hết hạn!");
            return "verify-otp";
        }
    }




}
