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

import java.time.LocalDateTime;
import java.util.Optional;


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
    public String loginPage(Model model,
                            @RequestParam(value = "success", required = false) String success) {

        model.addAttribute("loginRequest", new LoginRequest());

        if (success != null) {
            model.addAttribute("success", success);
        }

        return "login";
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

        Optional<User> optionalUser = userService.getUserByEmail(email); // thêm phương thức getUserByEmail()
        long secondsRemaining = 0;

        if(optionalUser.isPresent()) {
            User user = optionalUser.get();
            if(user.getLastOtpSentTime() != null) {
                long secondsSinceLast = java.time.Duration.between(user.getLastOtpSentTime(), LocalDateTime.now()).getSeconds();
                secondsRemaining = Math.max(60 - secondsSinceLast, 0);
            }
        }

        model.addAttribute("secondsRemaining", secondsRemaining); // gửi về HTML
        return "verify-otp";
    }

    // Xử lý submit OTP
    @PostMapping("/verify-otp")
    public String verifyOtpSubmit(@RequestParam String email,
                                  @RequestParam String otp,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {

        boolean success = userService.verifyOTP(email, otp);
        if (success) {
            // Thêm message vào flash attribute
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/api/v1/auth"; // chuyển về login
        } else {
            // OTP sai → không reset countdown
            Optional<User> optionalUser = userService.getUserByEmail(email);
            long secondsRemaining = 0;
            if(optionalUser.isPresent() && optionalUser.get().getLastOtpSentTime() != null) {
                long secondsSinceLast = java.time.Duration.between(optionalUser.get().getLastOtpSentTime(), LocalDateTime.now()).getSeconds();
                secondsRemaining = Math.max(60 - secondsSinceLast, 0);
            }

            model.addAttribute("secondsRemaining", secondsRemaining);
            model.addAttribute("email", email);
            model.addAttribute("message", "OTP không hợp lệ hoặc đã hết hạn!");
            return "verify-otp";
        }
    }



    @GetMapping("/resend-otp")
    public String resendOtp(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        String message = userService.resendOTP(email);

        if (message.startsWith("OTP mới")) {
            // Gửi OTP mới → thành công
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            // Chưa đủ 1 phút hoặc lỗi khác
            redirectAttributes.addFlashAttribute("message", message);
        }

        // Luôn redirect lại với email
        redirectAttributes.addAttribute("email", email);
        return "redirect:/api/v1/auth/verify-otp";
    }

    // Hiển thị form nhập email để quên mật khẩu
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("user", new User()); // nếu muốn dùng form binding
        return "forgot-password"; // tạo forgot-password.html
    }

    // Xử lý submit email nhận OTP reset
    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String email, RedirectAttributes redirectAttributes) {
        Optional<User> optionalUser = userService.getUserByEmail(email);
        if(optionalUser.isPresent()) {
            // Gửi OTP reset password
            String msg = userService.sendResetOTP(email);
            redirectAttributes.addFlashAttribute("success", msg);
            redirectAttributes.addAttribute("email", email);
            return "redirect:/api/v1/auth/reset-password";
        } else {
            redirectAttributes.addFlashAttribute("message", "Email không tồn tại!");
            return "redirect:/api/v1/auth/forgot-password";
        }
    }

    // Hiển thị form reset mật khẩu + OTP
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);

        Optional<User> optionalUser = userService.getUserByEmail(email);
        long secondsRemaining = 0;
        if(optionalUser.isPresent() && optionalUser.get().getLastOtpSentTime() != null) {
            long secondsSinceLast = java.time.Duration.between(optionalUser.get().getLastOtpSentTime(), LocalDateTime.now()).getSeconds();
            secondsRemaining = Math.max(60 - secondsSinceLast, 0);
        }

        model.addAttribute("secondsRemaining", secondsRemaining);
        return "reset-password";
    }

    // Xử lý submit OTP + mật khẩu mới
    @PostMapping("/reset-password")
    public String resetPasswordSubmit(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        boolean otpValid = userService.verifyOTP(email, otp);

        if(otpValid) {
            userService.updatePassword(email, newPassword);
            redirectAttributes.addFlashAttribute("success", "Mật khẩu đã được đặt lại thành công! Bạn có thể đăng nhập.");
            return "redirect:/api/v1/auth";
        } else {
            Optional<User> optionalUser = userService.getUserByEmail(email);
            long secondsRemaining = 0;
            if(optionalUser.isPresent() && optionalUser.get().getLastOtpSentTime() != null) {
                long secondsSinceLast = java.time.Duration.between(optionalUser.get().getLastOtpSentTime(), LocalDateTime.now()).getSeconds();
                secondsRemaining = Math.max(120 - secondsSinceLast, 0);
            }

            model.addAttribute("secondsRemaining", secondsRemaining);
            model.addAttribute("email", email);
            model.addAttribute("message", "OTP không hợp lệ hoặc đã hết hạn!");
            return "reset-password";
        }
    }

    // Resend OTP cho reset password
    @GetMapping("/resend-reset-otp")
    public String resendResetOtp(@RequestParam String email, RedirectAttributes redirectAttributes) {
        String message = userService.sendResetOTP(email);
        if(message.startsWith("OTP mới")) {
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            redirectAttributes.addFlashAttribute("message", message);
        }
        redirectAttributes.addAttribute("email", email);
        return "redirect:/api/v1/auth/reset-password";
    }

}
