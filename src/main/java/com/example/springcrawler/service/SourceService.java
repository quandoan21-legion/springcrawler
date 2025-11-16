package com.example.springcrawler.service;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.model.Source;
import com.example.springcrawler.repository.SourceRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class SourceService {

    private final SourceRepository sourceRepository;
    private final CategoryService categoryService;

    public SourceService(SourceRepository sourceRepository, CategoryService categoryService) {
        this.sourceRepository = sourceRepository;
        this.categoryService = categoryService;
    }
    public List<Source> getActiveSources() {
        return sourceRepository.findAllByDeletedFalseOrderByIdDesc();
    }

    public Source getSourceById(Long id) {
        Optional<Source> source = sourceRepository.findById(id);
        return source.orElse(null);
    }

    public Source createSource(Long categoryId, String url) {
        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }

        Source source = new Source();
        source.setCategory(category);
        source.setUrl(url);
        source.setDeleted(false);
        return sourceRepository.save(source);
    }

    public Source updateSource(Long id, Long categoryId, String url) {
        Source existing = getSourceById(id);
        if (existing == null || existing.isDeleted()) {
            return null;
        }

        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            return null;
        }

        existing.setCategory(category);
        existing.setUrl(url);
        return sourceRepository.save(existing);
    }

    public void softDeleteSource(Long id) {
        Source existing = getSourceById(id);
        if (existing != null && !existing.isDeleted()) {
            existing.setDeleted(true);
            sourceRepository.save(existing);
        }
    }
}
