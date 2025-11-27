package com.example.springcrawler.service;

import com.example.springcrawler.model.Post;
import com.example.springcrawler.model.Source;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SourceCrawlService {
    private final PostService postService;
    private final SourceService sourceService;

    private static final String DEFAULT_TITLE_SELECTOR = "h1";
    private static final String DEFAULT_CONTENT_SELECTOR = "article.fck_detail";
    private static final String DEFAULT_DESCRIPTION_SELECTOR = "p.description";
    private static final int BATCH_SIZE = 10;
    private static final long BOT_INTERVAL_MS = Duration.ofMinutes(5).toMillis();

    private final AtomicBoolean sourceBotRunning = new AtomicBoolean(false);
    private final AtomicBoolean crawlBotRunning = new AtomicBoolean(false);

    public SourceCrawlService(PostService postService, SourceService sourceService) {
        this.postService = postService;
        this.sourceService = sourceService;
    }

    @PostConstruct
    public void startBotsOnStartup() {
        runCrawlerBots();
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
                    post.setSourceUrl(url);
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
        List<Post> uncrawlPost = postService.getUnCrawlPosts(BATCH_SIZE);
        for (Post post : uncrawlPost) {
            String url = post.getCrawlUrl();
            if (!StringUtils.hasText(post.getSourceUrl()) && StringUtils.hasText(url)) {
                post.setSourceUrl(url);
            }
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

    public void runCrawlerBots() {
        startBotThread(sourceBotRunning, this::queueArticlesFromSourcesContinuously, "source-fetch-bot");
        startBotThread(crawlBotRunning, this::crawlUnCrawledPostsContinuously, "uncrawled-post-bot");
    }

    private void queueArticlesFromSourcesContinuously() {
        while (sourceBotRunning.get()) {
            queueArticlesFromSourcesBatch();
            pauseBot(sourceBotRunning);
        }
    }

    private void crawlUnCrawledPostsContinuously() {
        while (crawlBotRunning.get()) {
            crawlUnCrawlPost();
            pauseBot(crawlBotRunning);
        }
    }

    private void queueArticlesFromSourcesBatch() {
        int queuedCount = 0;
        List<Source> sources = sourceService.getActiveSources();
        for (Source source : sources) {
            if (source == null || !StringUtils.hasText(source.getUrl())) {
                continue;
            }
            try {
                String domain = extractDomain(source.getUrl());
                if (domain == null) {
                    continue;
                }
                Set<String> queuedLinks = crawlSourceForPostLink(source, source.getUrl().trim(), domain);
                queuedCount += queuedLinks.size();
                if (queuedCount >= BATCH_SIZE) {
                    break;
                }
            } catch (Exception ex) {
                System.err.println("Error queueing source " + source.getId() + ": " + ex.getMessage());
            }
        }
    }

    private void pauseBot(AtomicBoolean flag) {
        try {
            Thread.sleep(BOT_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            flag.set(false);
        }
    }

    private void startBotThread(AtomicBoolean flag, Runnable task, String threadName) {
        if (flag.compareAndSet(false, true)) {
            Thread thread = new Thread(() -> {
                try {
                    task.run();
                } finally {
                    flag.set(false);
                }
            }, threadName);
            thread.setDaemon(true);
            thread.start();
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
        if (document == null || !StringUtils.hasText(imageSelector)) {
            return null;
        }

        Element img = document.selectFirst(imageSelector);
        if (img == null) {
            return null;
        }

        String src = img.attr("src");
        return StringUtils.hasText(src) ? src : null;
    }

    private String resolveImageAttribute(Element element, String attribute) {
        if (!StringUtils.hasText(attribute)) {
            return null;
        }
        String value = element.absUrl(attribute);
        if (!StringUtils.hasText(value)) {
            value = element.attr(attribute);
        }
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if ("srcset".equals(attribute) || "data-srcset".equals(attribute)) {
            String[] candidates = value.split(",");
            if (candidates.length > 0) {
                return candidates[0].trim().split("\\s+")[0];
            }
        }
        return value;
    }

    private String extractDomain(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            return null;
        }
        return host.startsWith("www.") ? host.substring(4) : host;
    }
}
