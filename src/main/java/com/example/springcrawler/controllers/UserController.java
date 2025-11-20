package com.example.springcrawler.controllers;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.model.Post;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class UserController {

    private final PostService postService;
    private final CategoryService categoryService;

    public UserController(PostService postService, CategoryService categoryService) {
        this.postService = postService;
        this.categoryService = categoryService;
    }

    @GetMapping({"/", "/home"})
    public String showHome(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "latest") String sortOption,
            @RequestParam(value = "category", required = false) String categoryParam,
            Model model
    ) {
        Long categoryId = parseCategoryId(categoryParam);
        Sort sort = resolveSort(sortOption);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
        Page<Post> postsPage = postService.getPostsByStatus(Post.Status.CRAWLED, keyword, categoryId, pageable);
        List<Integer> pageNumbers = buildPageWindow(postsPage, 5);
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("title", "News Vendor Home");
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortOption", sortOption);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("categories", categories);
        model.addAttribute("postsPage", postsPage);
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("pageNumbers", pageNumbers);
        return "news-homepage-main/index";
    }

    private Long parseCategoryId(String param) {
        if (param == null || param.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(param);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Sort resolveSort(String sortOption) {
        if ("oldest".equalsIgnoreCase(sortOption)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private List<Integer> buildPageWindow(Page<Post> page, int windowSize) {
        if (page == null || page.getTotalPages() == 0) {
            return Collections.emptyList();
        }
        int totalPages = page.getTotalPages();
        int current = page.getNumber();
        int start = Math.max(0, current - windowSize / 2);
        int end = Math.min(totalPages - 1, start + windowSize - 1);
        start = Math.max(0, end - windowSize + 1);
        return IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
    }

    @GetMapping("/posts/{id}")
    public String showPostDetail(@PathVariable Long id, Model model) {
        Post post = postService.getPostById(id);
        if (post == null || post.getStatus() != Post.Status.CRAWLED) {
            return "redirect:/";
        }

        model.addAttribute("title", post.getTitle());
        model.addAttribute("post", post);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategoryId", post.getCategory() != null ? post.getCategory().getId() : null);
        return "news-homepage-main/detail";
    }
}
