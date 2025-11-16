package com.example.springcrawler.repository;
import com.example.springcrawler.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post,Long> {
    Optional<Post> findPostByCrawlUrl(String crawlUrl);
    @Query(
            value = "SELECT * FROM posts WHERE status = :status ORDER BY id ASC",
            nativeQuery = true
    )
    List<Post> findUncrawledPosts(
            @Param("status") String status,
            Pageable pageable
    );

    List<Post> findPostByStatus(Post.Status status);
}
