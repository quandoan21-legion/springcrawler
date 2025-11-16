package com.example.springcrawler.service;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.model.Post;
import com.example.springcrawler.repository.CategoryRepository;
import com.example.springcrawler.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;

    public PostService(PostRepository postRepository, CategoryRepository categoryRepository) {
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
    }

    // Lấy tất cả bài viết
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // Lấy bài viết theo ID
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


    // Tạo bài viết mới
    public Post createPost(String title, String content, Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        return postRepository.save(post);
    }


    // Cập nhật bài viết
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

    // Lưu bài viết (nếu cần dùng trực tiếp)
    public void savePost(Post post) {
        postRepository.save(post);
    }

    // Xóa bài viết
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}
