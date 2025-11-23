package com.example.springcrawler.repository;

import com.example.springcrawler.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 🔹 Lấy danh mục theo tên
    Optional<Category> findByName(String name);

    // 🔹 Kiểm tra danh mục theo tên
    boolean existsByName(String name);

    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
