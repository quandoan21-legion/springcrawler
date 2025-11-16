package com.example.springcrawler.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping("")
    public String adminPage(Model model) {
        model.addAttribute("adminName", "Admin");
        return "admin";
    }
}
