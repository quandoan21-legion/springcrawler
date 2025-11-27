package com.example.springcrawler.controllers.admin;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("")
    public String categoriesPage(Model model,
                                 @RequestParam(name = "keyword", required = false) String keyword,
                                 @RequestParam(name = "page", defaultValue = "0") int page,
                                 @RequestParam(name = "size", defaultValue = "10") int size) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 5), 50);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by("name").ascending());
        Page<Category> categoryPage = categoryService.getCategoriesPage(keyword, pageable);

        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", sanitizedSize);
        return "admin-categories";
    }

    @GetMapping("/add")
    public String addCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin-categories-add";
    }

    @PostMapping("/add")
    public String addCategory(@ModelAttribute Category category, Model model) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            model.addAttribute("error", "Category name cannot be empty.");
            return "admin-categories-add";
        }

        if (categoryService.existsByName(category.getName().trim())) {
            model.addAttribute("error", "Category name already exists.");
            return "admin-categories-add";
        }

        categoryService.createCategory(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", category);
        return "admin-categories-edit";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable Long id, @ModelAttribute Category category, Model model) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            model.addAttribute("error", "Category name cannot be empty.");
            model.addAttribute("category", categoryService.getCategoryById(id));
            return "admin-categories-edit";
        }

        Category existingByName = categoryService.getCategoryByName(category.getName().trim());
        if (existingByName != null && !existingByName.getId().equals(id)) {
            model.addAttribute("error", "Category name already exists.");
            model.addAttribute("category", categoryService.getCategoryById(id));
            return "admin-categories-edit";
        }

        Category existing = categoryService.getCategoryById(id);
        if (existing != null) {
            existing.setName(category.getName());
            existing.setDescription(category.getDescription());
            existing.setStatus(category.getStatus());
            categoryService.updateCategory(id, existing);
        }

        return "redirect:/admin/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/admin/categories";
    }
}
