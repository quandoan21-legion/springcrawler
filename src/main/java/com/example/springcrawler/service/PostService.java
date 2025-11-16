    package com.example.springcrawler.service;

    import com.example.springcrawler.model.Category;
    import com.example.springcrawler.model.Post;
    import com.example.springcrawler.repository.CategoryRepository;
    import com.example.springcrawler.repository.PostRepository;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;


    import java.util.List;

    @Service
    public class PostService {

        @Autowired
        private PostRepository postRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        // Lấy tất cả bài viết
        public List<Post> getAllPosts() {
            return postRepository.findAll();
        }

        // Lấy bài viết theo ID
        public Post getPostById(Long id) {
            return postRepository.findById(id).orElse(null);
        }

        // Tạo bài viết mới
        public Post createPost(String title, String content, Long categoryId) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
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
