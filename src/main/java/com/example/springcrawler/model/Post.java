package com.example.springcrawler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.springcrawler.model.Category;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String title;

    @Column(nullable = true, unique = true)
    private String slug; // URL thân thiện


    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    private String sourceUrl;
    private String tags;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private boolean uniqueContent = false;
    private String imgUrl = null;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    private String crawlUrl;

    public Post(String title, String slug, String content, String shortDescription, Category category) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.shortDescription = shortDescription;
        this.category = category;
    }

    public enum Status {
        DRAFT, UNCRAWL, CRAWLED, PUBLISHED, DELETED
    }
}
