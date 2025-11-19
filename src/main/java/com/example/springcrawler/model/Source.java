package com.example.springcrawler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sources")
@Data
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "title_selector")
    private String titleSelector;

    @Column(name = "content_selector")
    private String contentSelector;

    @Column(name = "description_selector")
    private String descriptionSelector;

    @Column(name = "image_selector")
    private String imageSelector;

    @Column(name = "removal_selector")
    private String removalSelector;
}
