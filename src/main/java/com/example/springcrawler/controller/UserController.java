package com.example.springcrawler.controller;

import com.example.springcrawler.entity.User;
import com.example.springcrawler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // Serve the HTML form
    @GetMapping("/create")
    public String getCreateUserPage() {
        return "create-user"; // Thymeleaf template
    }

    // Handle form submission
    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "User created successfully!");
        return "redirect:/api/users/create"; // redirect to avoid resubmission
    }

    // REST endpoints: return JSON
    @GetMapping
    @ResponseBody
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public User getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public User updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        return userRepository.save(user);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "User deleted successfully";
    }
}
