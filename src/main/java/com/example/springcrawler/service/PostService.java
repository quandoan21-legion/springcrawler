package com.example.springcrawler.service;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.model.Post;
import com.example.springcrawler.repository.CategoryRepository;
import com.example.springcrawler.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Collections;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    public PostService(PostRepository postRepository, CategoryRepository categoryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    // Fetch all posts
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Page<Post> searchPosts(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return postRepository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
        }
        return postRepository.findAll(pageable);
    }

    // Fetch a post by ID
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    public Post getPostByCrawlUrl(String url) {
        return postRepository.findPostByCrawlUrl(url).orElse(null);
    }
    public List<Post> getUnCrawlPosts() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        return postRepository.findUncrawledPosts(Post.Status.UNCRAWL.name(), pageable);
    }
    public List<Post> getUnCrawlPosts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findUncrawledPosts(Post.Status.UNCRAWL.name(), pageable);
    }

    public List<Post> getPostsByStatus(Post.Status status) {
        return postRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Page<Post> getPostsByStatus(Post.Status status, String keyword, Long categoryId, Pageable pageable) {
        return getPostsByStatuses(Collections.singletonList(status), keyword, categoryId, pageable);
    }

    public Page<Post> getPostsByStatuses(List<Post.Status> statuses, String keyword, Long categoryId, Pageable pageable) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (categoryId != null) {
            if (hasKeyword) {
                return postRepository.findByStatusInAndCategoryIdAndTitleContainingIgnoreCase(statuses, categoryId, keyword.trim(), pageable);
            }
            return postRepository.findByStatusInAndCategoryId(statuses, categoryId, pageable);
        }

        if (hasKeyword) {
            return postRepository.findByStatusInAndTitleContainingIgnoreCase(statuses, keyword.trim(), pageable);
        }
        return postRepository.findByStatusIn(statuses, pageable);
    }

    // Create a new post
    public Post createPost(String title, String content, Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Category does not exist"));
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        return postRepository.save(post);
    }


    // Update an existing post
    public Post updatePost(Long id, String title, String content, Long categoryId) {
        Post post = getPostById(id);
        if (post != null) {
            post.setTitle(title);
            post.setContent(content);

            Category category = categoryRepository.findById(categoryId).orElse(null);
            post.setCategory(category);

            return postRepository.save(post);
        }
        return null;
    }

    // Save a post directly
    public void savePost(Post post) {
        postRepository.save(post);
    }

    // Delete a post
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}
