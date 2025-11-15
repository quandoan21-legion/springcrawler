package com.example.springcrawler.controllers;

import com.example.springcrawler.dto.LoginRequest;
import com.example.springcrawler.model.User;
import com.example.springcrawler.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("api/v1/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    // Hiển thị trang login
    @GetMapping
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login"; // login.html
    }

    // Xử lý đăng nhập
    @PostMapping("/login")
    public String loginSubmit(@ModelAttribute LoginRequest loginRequest, Model model) {
//        String message = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
//        model.addAttribute("message", message);
//        model.addAttribute("loginRequest", loginRequest);
//        if(message.equals("Đăng nhập thành công")) {
//            return "redirect:/admin"; // redirect sang trang admin nếu login thành công
//        }
        return "login";
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




