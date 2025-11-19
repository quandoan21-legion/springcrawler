package com.example.springcrawler.controllers.admin;

import com.example.springcrawler.model.Category;
import com.example.springcrawler.model.Source;
import com.example.springcrawler.service.CategoryService;
import com.example.springcrawler.service.SourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/sources")
public class AdminSourceController {

    private final SourceService sourceService;
    private final CategoryService categoryService;

    public AdminSourceController(SourceService sourceService, CategoryService categoryService) {
        this.sourceService = sourceService;
        this.categoryService = categoryService;
    }

    @GetMapping("")
    public String listSources(Model model) {
        model.addAttribute("sources", sourceService.getActiveSources());
        return "admin-sources";
    }

    @GetMapping("/add")
    public String addSourceForm(Model model) {
        model.addAttribute("source", new Source());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-sources-add";
    }

    @PostMapping("/add")
    public String addSource(@RequestParam Long categoryId,
                            @RequestParam String url,
                            @RequestParam(required = false) String titleSelector,
                            @RequestParam(required = false) String contentSelector,
                            @RequestParam(required = false) String descriptionSelector,
                            @RequestParam(required = false) String imageSelector,
                            @RequestParam(required = false) String removalSelector,
                            Model model) {
        Source formSource = buildSourceDraft(null, categoryId, url, titleSelector, contentSelector,
                descriptionSelector, imageSelector, removalSelector);

        if (!StringUtils.hasText(url)) {
            model.addAttribute("error", "URL không được để trống");
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("source", formSource);
            return "admin-sources-add";
        }

        try {
            sourceService.createSource(categoryId, url.trim(), titleSelector, contentSelector,
                    descriptionSelector, imageSelector, removalSelector);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("source", formSource);
            return "admin-sources-add";
        }

        return "redirect:/admin/sources";
    }

    @GetMapping("/edit/{id}")
    public String editSourceForm(@PathVariable Long id, Model model) {
        Source source = sourceService.getSourceById(id);
        if (source == null || source.isDeleted()) {
            return "redirect:/admin/sources";
        }
        model.addAttribute("source", source);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin-sources-edit";
    }

    @PostMapping("/edit/{id}")
    public String editSource(@PathVariable Long id,
                             @RequestParam Long categoryId,
                             @RequestParam String url,
                             @RequestParam(required = false) String titleSelector,
                             @RequestParam(required = false) String contentSelector,
                             @RequestParam(required = false) String descriptionSelector,
                             @RequestParam(required = false) String imageSelector,
                             @RequestParam(required = false) String removalSelector,
                             Model model) {
        Source existing = sourceService.getSourceById(id);
        if (existing == null || existing.isDeleted()) {
            return "redirect:/admin/sources";
        }

        if (!StringUtils.hasText(url)) {
            model.addAttribute("error", "URL không được để trống");
            Source draft = buildSourceDraft(id, categoryId, url, titleSelector, contentSelector,
                    descriptionSelector, imageSelector, removalSelector);
            if (draft.getCategory() == null) {
                draft.setCategory(existing.getCategory());
            }
            model.addAttribute("source", draft);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-sources-edit";
        }

        Source updated = sourceService.updateSource(id, categoryId, url.trim(), titleSelector,
                contentSelector, descriptionSelector, imageSelector, removalSelector);
        if (updated == null) {
            model.addAttribute("error", "Nguồn không tồn tại hoặc danh mục không hợp lệ");
            Source draft = buildSourceDraft(id, categoryId, url, titleSelector, contentSelector,
                    descriptionSelector, imageSelector, removalSelector);
            if (draft.getCategory() == null) {
                draft.setCategory(existing.getCategory());
            }
            model.addAttribute("source", draft);
            model.addAttribute("categories", categoryService.getAllCategories());
            return "admin-sources-edit";
        }

        return "redirect:/admin/sources";
    }

    @GetMapping("/delete/{id}")
    public String softDeleteSource(@PathVariable Long id) {
        sourceService.softDeleteSource(id);
        return "redirect:/admin/sources";
    }

    private Source buildSourceDraft(Long sourceId,
                                    Long categoryId,
                                    String url,
                                    String titleSelector,
                                    String contentSelector,
                                    String descriptionSelector,
                                    String imageSelector,
                                    String removalSelector) {
        Source draft = new Source();
        draft.setId(sourceId);
        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            if (category != null) {
                draft.setCategory(category);
            }
        }
        draft.setUrl(url);
        draft.setTitleSelector(titleSelector);
        draft.setContentSelector(contentSelector);
        draft.setDescriptionSelector(descriptionSelector);
        draft.setImageSelector(imageSelector);
        draft.setRemovalSelector(removalSelector);
        return draft;
    }
}
