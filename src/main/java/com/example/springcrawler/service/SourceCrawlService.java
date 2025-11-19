package com.example.springcrawler.service;

import com.example.springcrawler.model.Post;
import com.example.springcrawler.model.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SourceCrawlService {
    private final PostService postService;

    private static final String DEFAULT_TITLE_SELECTOR = "h1";
    private static final String DEFAULT_CONTENT_SELECTOR = "article.fck_detail";
    private static final String DEFAULT_DESCRIPTION_SELECTOR = "p.description";

    public SourceCrawlService(PostService postService) {
        this.postService = postService;
    }

    public Set<String> crawlSourceForPostLink(Source source, String categoryUrl, String siteUrl) {
        Set<String> articleLinks = new HashSet<>();
        try {
            Document doc = Jsoup.connect(categoryUrl)
                    .timeout(5000)
                    .get();

            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String url = link.absUrl("href");
                if (url.contains(siteUrl) && url.contains(".html")) {
                    Post existing = postService.getPostByCrawlUrl(url);
                    if (existing != null) {
                        continue;
                    }
                    Post post = new Post();
                    post.setCrawlUrl(url);
                    post.setStatus(Post.Status.UNCRAWL);
                    post.setCategory(source.getCategory());
                    post.setSource(source);
                    postService.savePost(post);
                    articleLinks.add(url);
                }
            }

        } catch (IOException e) {
            System.err.println("Error crawling category: " + e.getMessage());
        }

        return articleLinks;
    }

    public void crawlUnCrawlPost() {
        List<Post> uncrawlPost = postService.getUnCrawlPosts(10);
        for (Post post : uncrawlPost) {
            String url = post.getCrawlUrl();
            try {
                Document doc = Jsoup.connect(url)
                        .timeout(5000)
                        .get();

                Source source = post.getSource();
                String titleSelector = resolveSelector(source != null ? source.getTitleSelector() : null,
                        DEFAULT_TITLE_SELECTOR);
                String contentSelector = resolveSelector(source != null ? source.getContentSelector() : null,
                        DEFAULT_CONTENT_SELECTOR);
                String descriptionSelector = resolveSelector(source != null ? source.getDescriptionSelector() : null,
                        DEFAULT_DESCRIPTION_SELECTOR);
                String imageSelector = source != null ? source.getImageSelector() : null;
                String removalSelector = source != null ? source.getRemovalSelector() : null;

                Element titleElement = selectFirst(doc, titleSelector);
                Element contentElement = selectFirst(doc, contentSelector);
                Element descriptionElement = selectFirst(doc, descriptionSelector);

                if (contentElement != null && StringUtils.hasText(removalSelector)) {
                    contentElement.select(removalSelector).remove();
                }

                post.setTitle(titleElement != null ? titleElement.text() : null);
                post.setContent(contentElement != null ? contentElement.text() : null);
                post.setShortDescription(descriptionElement != null ? descriptionElement.text() : null);

                String imageUrl = extractImageUrl(doc, imageSelector);
                if (StringUtils.hasText(imageUrl)) {
                    post.setImgUrl(imageUrl);
                }

                post.setStatus(Post.Status.CRAWLED);
                postService.savePost(post);
            } catch (IOException e) {
                System.err.println("Error crawling category: " + e.getMessage());
            }

        }
    }

    private String resolveSelector(String candidate, String fallback) {
        return StringUtils.hasText(candidate) ? candidate.trim() : fallback;
    }

    private Element selectFirst(Document document, String selector) {
        if (!StringUtils.hasText(selector)) {
            return null;
        }
        return document.select(selector).first();
    }

    private String extractImageUrl(Document document, String imageSelector) {
        if (!StringUtils.hasText(imageSelector)) {
            return null;
        }
        Element imageElement = document.select(imageSelector).first();
        if (imageElement == null) {
            return null;
        }
        String src = imageElement.absUrl("src");
        if (!StringUtils.hasText(src)) {
            src = imageElement.attr("src");
        }
        if (!StringUtils.hasText(src)) {
            src = imageElement.absUrl("data-src");
        }
        return StringUtils.hasText(src) ? src : null;
    }
}
