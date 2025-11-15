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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


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

    // Xử lý đăng ký
    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute User user, Model model) {
        String message = userService.register(user);
        model.addAttribute("message", message);
        model.addAttribute("user", user);
        if(message.equals("Đăng ký thành công")) {
            return "redirect:/api/v1/auth"; // redirect sang login sau khi đăng ký thành công
        }
        return "register";
    }
}
