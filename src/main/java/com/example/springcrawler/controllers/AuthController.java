package com.example.springcrawler.controllers;

import com.example.springcrawler.dto.LoginRequest;
import com.example.springcrawler.model.User;
import com.example.springcrawler.service.UserService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


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
        boolean success = userService.registerWithOTP(user); // sử dụng service mới
        logger.info("User email: {}", user.getEmail());

        if (success) {
            // nếu đăng ký thành công hoặc email đã tồn tại nhưng chưa verify → chuyển sang form OTP
            return "redirect:/api/v1/auth/verify-otp?email=" + user.getEmail();
        } else {
            // email đã tồn tại và đã verify
            model.addAttribute("message", "Email đã tồn tại hoặc đang chờ xác thực!");
            model.addAttribute("user", user);
            return "register"; // quay lại form đăng ký
        }
    }



    // Hiển thị form nhập OTP
    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp"; // verify-otp.html
    }
    // Gửi lại OTP
    @GetMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, Model model) {
        String message = userService.resendOTP(email);
        model.addAttribute("email", email);
        model.addAttribute("message", message);
        return "verify-otp"; // Quay lại trang nhập OTP
    }

    // Xử lý submit OTP
    @PostMapping("/verify-otp")
    public String verifyOtpSubmit(@RequestParam String email,
                                  @RequestParam String otp,
                                  RedirectAttributes redirectAttributes) {

        boolean success = userService.verifyOTP(email, otp);
        if (success) {
            // OTP hợp lệ → redirect về login với message success
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công!");
            return "redirect:/api/v1/auth";
        } else {
            // OTP sai → redirect lại form OTP với message lỗi
            redirectAttributes.addFlashAttribute("error", "OTP không hợp lệ hoặc đã hết hạn!");
            redirectAttributes.addAttribute("email", email); // để vẫn show email trên form
            return "redirect:/api/v1/auth/verify-otp";
        }
    }






}
