package com.example.springcrawler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.t2404e.spring_prj.model.Category;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 🔹 Lấy danh mục theo tên
    Optional<Category> findByName(String name);

    // 🔹 Kiểm tra danh mục theo tên
    boolean existsByName(String name);
}
