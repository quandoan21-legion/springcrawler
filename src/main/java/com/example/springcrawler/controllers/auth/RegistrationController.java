package com.example.springcrawler.controllers.auth;

import com.example.springcrawler.model.User;
import com.example.springcrawler.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);
    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute User user,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (user.getPassword() == null || !user.getPassword().equals(confirmPassword)) {
            model.addAttribute("message", "Password and confirmation do not match.");
            model.addAttribute("user", user);
            return "register";
        }

        boolean success = userService.registerWithOTP(user);
        logger.info("User email: {}", user.getEmail());

        if (success) {
            redirectAttributes.addAttribute("email", user.getEmail());
            return "redirect:/api/v1/auth/verify-otp";
        } else {
            model.addAttribute("message", "Email already exists or is pending verification.");
            model.addAttribute("user", user);
            return "register";
        }
    }
}
