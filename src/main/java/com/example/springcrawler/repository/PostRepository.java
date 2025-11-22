package com.example.springcrawler.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.t2404e.spring_prj.model.Post;


public interface PostRepository extends JpaRepository<Post,Long> {
}
