package com.example.springcrawler.controllers.admin;

import com.example.springcrawler.model.Post;
import com.example.springcrawler.model.Category;
import com.example.springcrawler.service.CategoryService;
import com.example.springcrawler.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;

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
    public String postsPage(@RequestParam(name = "search", required = false) String search,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            @RequestParam(name = "size", defaultValue = "10") int size,
                            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
                            @RequestParam(name = "direction", defaultValue = "desc") String direction,
                            Model model) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 5), 50);

        Sort sortSpec = buildSort(sort, direction);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sortSpec);
        Page<Post> postPage = postService.searchPosts(search, pageable);

        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("page", postPage);
        model.addAttribute("postsPage", postPage);
        model.addAttribute("search", search);
        model.addAttribute("size", sanitizedSize);
        model.addAttribute("sort", resolveSortField(sort));
        model.addAttribute("direction", resolveDirection(direction));
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
                          @RequestParam(required = false) String slug,
                          @RequestParam String content,
                          @RequestParam Long categoryId,
                          @RequestParam(required = false) String shortDescription,
                          @RequestParam(name = "imgUrl", required = false) String imgUrl,
                          @RequestParam(required = false) String sourceUrl,
                          @RequestParam(required = false) String tags,
                          @RequestParam(required = false) Post.Status status,
                          @RequestParam(required = false) Boolean uniqueContent,
                          @RequestParam(required = false) String seoTitle,
                          @RequestParam(required = false) String seoDescription,
                          @RequestParam(required = false) String seoKeywords,
                          Model model) {

        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ tiêu đề và nội dung!");
            model.addAttribute("categories", categoryService.getAllCategories());
            populatePostFormModel(model, title, slug, content, categoryId, shortDescription, imgUrl, sourceUrl, tags, status,
                    uniqueContent, seoTitle, seoDescription, seoKeywords);
            return "admin-post-add";
        }

        try {
            Post post = postService.createPost(title.trim(), content.trim(), categoryId);
            post.setSlug(StringUtils.hasText(slug) ? slug.trim() : null);
            post.setShortDescription(shortDescription);
            post.setImgUrl(imgUrl);
            post.setSourceUrl(sourceUrl);
            post.setTags(tags);
            post.setStatus(status != null ? status : Post.Status.DRAFT);
            post.setUniqueContent(Boolean.TRUE.equals(uniqueContent));
            post.setSeoTitle(seoTitle);
            post.setSeoDescription(seoDescription);
            post.setSeoKeywords(seoKeywords);

            postService.savePost(post);

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            populatePostFormModel(model, title, slug, content, categoryId, shortDescription, imgUrl, sourceUrl, tags, status,
                    uniqueContent, seoTitle, seoDescription, seoKeywords);
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
                           @RequestParam(required = false) String slug,
                           @RequestParam String content,
                           @RequestParam Long categoryId,
                           @RequestParam(required = false) String shortDescription,
                           @RequestParam(name = "imgUrl", required = false) String imgUrl,
                           @RequestParam(required = false) String sourceUrl,
                           @RequestParam(required = false) String tags,
                           @RequestParam(required = false) Post.Status status,
                           @RequestParam(required = false) Boolean uniqueContent,
                           @RequestParam(required = false) String seoTitle,
                           @RequestParam(required = false) String seoDescription,
                           @RequestParam(required = false) String seoKeywords,
                           Model model) {

        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
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

        post.setSlug(StringUtils.hasText(slug) ? slug.trim() : null);
        post.setShortDescription(shortDescription);
        post.setImgUrl(imgUrl);
        post.setSourceUrl(sourceUrl);
        post.setTags(tags);
        post.setStatus(status != null ? status : Post.Status.DRAFT);
        post.setUniqueContent(Boolean.TRUE.equals(uniqueContent));
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

    private void populatePostFormModel(Model model,
                                       String title,
                                       String slug,
                                       String content,
                                       Long categoryId,
                                       String shortDescription,
                                       String imgUrl,
                                       String sourceUrl,
                                       String tags,
                                       Post.Status status,
                                       Boolean uniqueContent,
                                       String seoTitle,
                                       String seoDescription,
                                       String seoKeywords) {
        Post formPost = new Post();
        formPost.setTitle(title);
        formPost.setSlug(slug);
        formPost.setContent(content);
        formPost.setShortDescription(shortDescription);
        formPost.setImgUrl(imgUrl);
        formPost.setSourceUrl(sourceUrl);
        formPost.setTags(tags);
        if (categoryId != null) {
            Category category = new Category();
            category.setId(categoryId);
            formPost.setCategory(category);
        }
        formPost.setStatus(status);
        formPost.setUniqueContent(Boolean.TRUE.equals(uniqueContent));
        formPost.setSeoTitle(seoTitle);
        formPost.setSeoDescription(seoDescription);
        formPost.setSeoKeywords(seoKeywords);
        model.addAttribute("post", formPost);
    }

    private Sort buildSort(String sortField, String direction) {
        String field = resolveSortField(sortField);
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private String resolveSortField(String requested) {
        if (requested == null) {
            return "createdAt";
        }
        return switch (requested) {
            case "title" -> "title";
            case "status" -> "status";
            case "id" -> "id";
            case "updatedAt" -> "updatedAt";
            default -> "createdAt";
        };
    }

    private String resolveDirection(String requested) {
        return "asc".equalsIgnoreCase(requested) ? "asc" : "desc";
    }
}
