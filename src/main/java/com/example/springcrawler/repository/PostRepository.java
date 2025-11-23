package com.example.springcrawler.repository;
import com.example.springcrawler.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PostRepository extends JpaRepository<Post,Long> {
    Optional<Post> findPostByCrawlUrl(String crawlUrl);
    Page<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
    @Query(
            value = "SELECT * FROM posts WHERE status = :status ORDER BY id ASC",
            nativeQuery = true
    )
    List<Post> findUncrawledPosts(
            @Param("status") String status,
            Pageable pageable
    );

    List<Post> findPostByStatus(Post.Status status);

    List<Post> findByStatusOrderByCreatedAtDesc(Post.Status status);

    Page<Post> findByStatus(Post.Status status, Pageable pageable);

    Page<Post> findByStatusAndTitleContainingIgnoreCase(Post.Status status, String keyword, Pageable pageable);

    Page<Post> findByStatusAndCategoryId(Post.Status status, Long categoryId, Pageable pageable);

    Page<Post> findByStatusAndCategoryIdAndTitleContainingIgnoreCase(Post.Status status, Long categoryId, String keyword, Pageable pageable);

    Page<Post> findByStatusIn(List<Post.Status> statuses, Pageable pageable);

    Page<Post> findByStatusInAndTitleContainingIgnoreCase(List<Post.Status> statuses, String keyword, Pageable pageable);

    Page<Post> findByStatusInAndCategoryId(List<Post.Status> statuses, Long categoryId, Pageable pageable);

    Page<Post> findByStatusInAndCategoryIdAndTitleContainingIgnoreCase(List<Post.Status> statuses, Long categoryId, String keyword, Pageable pageable);
}
