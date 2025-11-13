package com.example.springcrawler.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping("/admin")
    public String adminPage(Model model) {
        // Bạn có thể thêm thông tin user, thống kê, bài viết, v.v...
        model.addAttribute("adminName", "Admin"); // tạm thời
        return "admin"; // admin.html
    }
}
