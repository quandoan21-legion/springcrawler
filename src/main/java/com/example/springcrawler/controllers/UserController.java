package com.example.springcrawler.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1")
public class UserController {

    @GetMapping("/home")
    public String showHome(Model model) {
        model.addAttribute("title", "News Vendor Home");

        return "frontends/News-Website-Template/index";
    }
}
