package com.example.springcrawler.repository;
import com.example.springcrawler.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PostRepository extends JpaRepository<Post,Long> {
}
