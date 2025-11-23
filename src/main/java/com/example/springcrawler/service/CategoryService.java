package com.example.springcrawler.service;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // Lấy tất cả danh mục
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Page<Category> getCategoriesPage(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return categoryRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }
        return categoryRepository.findAll(pageable);
    }

    // Lấy danh mục theo id
    public Category getCategoryById(Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        return category.orElse(null);
    }

    //  Lấy danh mục theo tên
    public Category getCategoryByName(String name) {
        Optional<Category> category = categoryRepository.findByName(name);
        return category.orElse(null);
    }

    // Kiểm tra tên danh mục đã tồn tại
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    // Tạo danh mục mới
    public void createCategory(Category category) {
        categoryRepository.save(category);
    }

    // Cập nhật danh mục
    public void updateCategory(Long id, Category updatedCategory) {
        Category category = getCategoryById(id);
        if (category != null) {
            category.setName(updatedCategory.getName());
            category.setDescription(updatedCategory.getDescription());
            category.setStatus(updatedCategory.getStatus());
            categoryRepository.save(category);
        }
    }

    // Xóa danh mục
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
