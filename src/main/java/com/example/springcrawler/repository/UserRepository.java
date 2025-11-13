package com.example.springcrawler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.t2404e.spring_prj.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
//    tim user bang email
    Optional<User> findByEmail(String Email);
    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
}
