package com.example.springcrawler.repository;

import com.example.springcrawler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
//    tim user bang email
    Optional<User> findByEmail(String Email);
    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email);

}
