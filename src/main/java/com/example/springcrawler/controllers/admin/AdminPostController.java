package com.example.springcrawler.controllers.admin;

import com.example.springcrawler.model.Post;
import com.example.springcrawler.service.CategoryService;
import com.example.springcrawler.service.PostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/posts")
public class AdminPostController {

    private final PostService postService;
    private final CategoryService categoryService;

    public AdminPostController(PostService postService, CategoryService categoryService) {
        this.postService = postService;
        this.categoryService = categoryService;
    }

    @GetMapping("")
    public String postsPage(Model model) {
        model.addAttribute("posts", postService.getAllPosts());
        return "admin-posts";
    }

    @GetMapping("/add")
    public String addPostForm(Model model) {
        model.addAttribute("post", new Post());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-post-add";
    }

    @PostMapping("/add")
    public String addPost(@RequestParam String title,
                          @RequestParam String content,
                          @RequestParam Long categoryId,
                          @RequestParam(required = false) String shortDescription,
                          @RequestParam(required = false) String sourceUrl,
                          @RequestParam(required = false) String tags,
                          @RequestParam(required = false) Post.Status status,
                          @RequestParam(required = false) Boolean uniqueContent,
                          @RequestParam(required = false) String seoTitle,
                          @RequestParam(required = false) String seoDescription,
                          @RequestParam(required = false) String seoKeywords,
                          Model model) {

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ tiêu đề và nội dung!");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-add";
        }

        try {
            Post post = postService.createPost(title.trim(), content.trim(), categoryId);
            post.setShortDescription(shortDescription);
            post.setSourceUrl(sourceUrl);
            post.setTags(tags);
            post.setStatus(status != null ? status : Post.Status.DRAFT);
            post.setUniqueContent(uniqueContent != null ? uniqueContent : false);
            post.setSeoTitle(seoTitle);
            post.setSeoDescription(seoDescription);
            post.setSeoKeywords(seoKeywords);

            postService.savePost(post);

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-add";
        }

        return "redirect:/admin/posts";
    }

    @GetMapping("/edit/{id}")
    public String editPostForm(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return "redirect:/admin/posts";
        }
        model.addAttribute("post", post);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-post-edit";
    }

    @PostMapping("/edit/{id}")
    public String editPost(@PathVariable Long id,
                           @RequestParam String title,
                           @RequestParam String content,
                           @RequestParam Long categoryId,
                           @RequestParam(required = false) String shortDescription,
                           @RequestParam(required = false) String sourceUrl,
                           @RequestParam(required = false) String tags,
                           @RequestParam(required = false) Post.Status status,
                           @RequestParam(required = false) Boolean uniqueContent,
                           @RequestParam(required = false) String seoTitle,
                           @RequestParam(required = false) String seoDescription,
                           @RequestParam(required = false) String seoKeywords,
                           Model model) {

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ tiêu đề và nội dung!");
            model.addAttribute("post", postService.getPostById(id));
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-edit";
        }

        Post post = postService.updatePost(id, title.trim(), content.trim(), categoryId);

        if (post == null) {
            model.addAttribute("error", "Bài viết hoặc danh mục không tồn tại!");
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-post-edit";
        }

        post.setShortDescription(shortDescription);
        post.setSourceUrl(sourceUrl);
        post.setTags(tags);
        post.setStatus(status != null ? status : Post.Status.DRAFT);
        post.setUniqueContent(uniqueContent != null ? uniqueContent : post.isUniqueContent());
        post.setSeoTitle(seoTitle);
        post.setSeoDescription(seoDescription);
        post.setSeoKeywords(seoKeywords);

        postService.savePost(post);

        return "redirect:/admin/posts";
    }

    @GetMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "redirect:/admin/posts";
    }
}
