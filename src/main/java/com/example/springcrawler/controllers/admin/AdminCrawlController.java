package com.example.springcrawler.controllers.admin;

import com.example.springcrawler.model.Source;
import com.example.springcrawler.service.PostService;
import com.example.springcrawler.service.SourceCrawlService;
import com.example.springcrawler.service.SourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Controller
@RequestMapping("/admin/crawl")
public class AdminCrawlController {

    private final SourceCrawlService sourceCrawlService;
    private final SourceService sourceService;
    private final PostService postService;

    public AdminCrawlController(SourceCrawlService sourceCrawlService, SourceService sourceService, PostService postService) {
        this.sourceCrawlService = sourceCrawlService;
        this.sourceService = sourceService;
        this.postService = postService;
    }

    public static String getDomain(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        if (host == null) return null;

        // Remove "www." prefix if present
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        return host;
    }

    @GetMapping("")
    public String showCrawlForm(Model model) {
        model.addAttribute("sources", sourceService.getActiveSources());
        return "admin-crawl";
    }

    @PostMapping("")
    public String crawlCategory(@RequestParam("sourceId") Long sourceId,
                                Model model) throws URISyntaxException {
        model.addAttribute("sources", sourceService.getActiveSources());
        model.addAttribute("selectedSourceId", sourceId);

        Source source = sourceService.getSourceById(sourceId);
        if (source == null) {
            model.addAttribute("error", "Source does not exist or has been removed.");
            return "admin-crawl";
        }

        String sourceUrl = source.getUrl();
        if (sourceUrl == null || sourceUrl.trim().isEmpty()) {
            model.addAttribute("error", "Source is missing a valid URL.");
            return "admin-crawl";
        }

        Set<String> links = sourceCrawlService.crawlSourceForPostLink(source, sourceUrl.trim(), getDomain(sourceUrl));
        model.addAttribute("links", links);
        model.addAttribute("sourceUrl", sourceUrl);
        return "admin-crawl";
    }

    @PostMapping("/run-pending")
    public String runPendingPosts(Model model) {
        model.addAttribute("sources", sourceService.getActiveSources());
        sourceCrawlService.crawlUnCrawlPost();
        model.addAttribute("message", "Processing UNCRAWL posts.");
        return "admin-crawl";
    }
}
