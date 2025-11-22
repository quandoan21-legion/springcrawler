package com.example.springcrawler.controllers;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.t2404e.spring_prj.model.Category;
import org.t2404e.spring_prj.model.Post;
import org.t2404e.spring_prj.model.User;
import org.t2404e.spring_prj.service.CategoryService;
import org.t2404e.spring_prj.service.PostService;
import org.t2404e.spring_prj.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CategoryService categoryService;

    // ================== TRANG ADMIN CHÍNH ==================
    @GetMapping("")
    public String adminPage(Model model) {
        model.addAttribute("adminName", "Admin");
        return "admin";
    }

    // ================== QUẢN LÝ NGƯỜI DÙNG ==================
    @GetMapping("/users")
    public String usersPage(Model model, @RequestParam(required = false) String keyword) {
        List<User> users;
        if (keyword != null && !keyword.isEmpty()) {
            users = userService.searchUsers(keyword);
        } else {
            users = userService.getAllUsers();
        }
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        return "admin-users";
    }

    // Form chỉnh sửa user
    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        return "admin-users-edit";
    }

    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Long id,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam(required = false) String password,
                           Model model) {
        try {
            userService.updateUser(id, fullName, email, password);
            model.addAttribute("message", "Cập nhật thành công");
        } catch (Exception e) {
            model.addAttribute("error", "Cập nhật thất bại");
            return "admin-users-edit"; // quay lại form edit
        }
        return "redirect:/admin/users";
    }


    // Xóa user
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }

    // ================== QUẢN LÝ DANH MỤC ==================
    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "admin-categories"; // chắc chắn file admin-categories.html tồn tại
    }

    @GetMapping("/categories/add")
    public String addCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin-categories-add"; // admin-categories-add.html
    }

    @PostMapping("/categories/add")
    public String addCategory(@ModelAttribute Category category, Model model) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            model.addAttribute("error", "Tên danh mục không được để trống");
            return "admin-categories-add";
        }

        // Kiểm tra trùng tên
        if (categoryService.existsByName(category.getName().trim())) {
            model.addAttribute("error", "Tên danh mục đã tồn tại");
            return "admin-categories-add";
        }

        categoryService.createCategory(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", category);
        return "admin-categories-edit"; // admin-categories-edit.html
    }

    @PostMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Long id, @ModelAttribute Category category, Model model) {
        // Kiểm tra tên không được trống
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            model.addAttribute("error", "Tên danh mục không được để trống");
            model.addAttribute("category", categoryService.getCategoryById(id));
            return "admin-categories-edit";
        }

        // Kiểm tra trùng tên (ngoại trừ chính category này)
        Category existingByName = categoryService.getCategoryByName(category.getName().trim());
        if (existingByName != null && !existingByName.getId().equals(id)) {
            model.addAttribute("error", "Tên danh mục đã tồn tại");
            model.addAttribute("category", categoryService.getCategoryById(id));
            return "admin-categories-edit";
        }

        // Lấy category hiện tại và cập nhật các trường
        Category existing = categoryService.getCategoryById(id);
        if (existing != null) {
            existing.setName(category.getName());
            existing.setDescription(category.getDescription());
            existing.setStatus(category.getStatus());
            categoryService.updateCategory(id, existing);
        }

        return "redirect:/admin/categories";
    }


    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/admin/categories";
    }


    // ================== QUẢN LÝ BÀI VIẾT ==================
    @GetMapping("/posts")
    public String postsPage(Model model) {
        model.addAttribute("posts", postService.getAllPosts());
        return "admin-posts";
    }

    @GetMapping("/posts/add")
    public String addPostForm(Model model) {
        model.addAttribute("post", new Post());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-post-add";
    }

    @PostMapping("/posts/add")
    public String addPost(@RequestParam String title,
                          @RequestParam String content,
                          @RequestParam Long categoryId,
                          @RequestParam(required = false) String shortDescription,
                          @RequestParam(required = false) String sourceUrl,
                          @RequestParam(required = false) String tags,
                          @RequestParam(required = false) Post.Status status,
                          @RequestParam(required = false) Boolean uniqueContent,
                          @RequestParam(required = false) String seoTitle,
                          @RequestParam(required = false) String seoDescription,
                          @RequestParam(required = false) String seoKeywords,
                          Model model) {

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ tiêu đề và nội dung!");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-add";
        }

        try {
            // Tạo post mới, slug được sinh tự động trong PostService
            Post post = postService.createPost(title.trim(), content.trim(), categoryId);

            // Set các trường phụ
            post.setShortDescription(shortDescription);
            post.setSourceUrl(sourceUrl);
            post.setTags(tags);
            post.setStatus(status != null ? status : Post.Status.DRAFT);
            post.setUniqueContent(uniqueContent != null ? uniqueContent : false);
            post.setSeoTitle(seoTitle);
            post.setSeoDescription(seoDescription);
            post.setSeoKeywords(seoKeywords);

            postService.savePost(post);

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-add";
        }

        return "redirect:/admin/posts";
    }

    @GetMapping("/posts/edit/{id}")
    public String editPostForm(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return "redirect:/admin/posts";
        }
        model.addAttribute("post", post);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-post-edit";
    }

    @PostMapping("/posts/edit/{id}")
    public String editPost(@PathVariable Long id,
                           @RequestParam String title,
                           @RequestParam String content,
                           @RequestParam Long categoryId,
                           @RequestParam(required = false) String shortDescription,
                           @RequestParam(required = false) String sourceUrl,
                           @RequestParam(required = false) String tags,
                           @RequestParam(required = false) Post.Status status,
                           @RequestParam(required = false) Boolean uniqueContent,
                           @RequestParam(required = false) String seoTitle,
                           @RequestParam(required = false) String seoDescription,
                           @RequestParam(required = false) String seoKeywords,
                           Model model) {

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ tiêu đề và nội dung!");
            model.addAttribute("post", postService.getPostById(id));
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-edit";
        }

        Post post = postService.updatePost(id, title.trim(), content.trim(), categoryId);

        if (post == null) {
            model.addAttribute("error", "Bài viết hoặc danh mục không tồn tại!");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-edit";
        }

        // Set các trường phụ
        post.setShortDescription(shortDescription);
        post.setSourceUrl(sourceUrl);
        post.setTags(tags);
        post.setStatus(status != null ? status : Post.Status.DRAFT);
        post.setUniqueContent(uniqueContent != null ? uniqueContent : post.isUniqueContent());
        post.setSeoTitle(seoTitle);
        post.setSeoDescription(seoDescription);
        post.setSeoKeywords(seoKeywords);

        postService.savePost(post);

        return "redirect:/admin/posts";
    }

    @GetMapping("/posts/delete/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "redirect:/admin/posts";
    }

}
