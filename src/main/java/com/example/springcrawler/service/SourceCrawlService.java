package com.example.springcrawler.service;

import com.example.springcrawler.model.Post;
import com.example.springcrawler.model.Source;
import com.example.springcrawler.repository.PostRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Service
public class SourceCrawlService {
    private final PostService postService;
    public SourceCrawlService(PostService postService) {
        this.postService = postService;
    }

    public Set<String> crawlSourceForPostLink(Source source, String categoryUrl, String siteUrl) {
        Set<String> articleLinks = new HashSet<>();
        try {
            Document doc = Jsoup.connect(categoryUrl)
                    .timeout(5000)
                    .get();

            Elements links = doc.select("a[href]"); // Lấy tất cả a[href]
            for (Element link : links) {
                String url = link.absUrl("href");
                if (url.contains(siteUrl) && url.contains(".html") ) {
                    Post existing = postService.getPostByCrawlUrl(url);
                    if (existing != null) {
                        continue;
                    }
                    Post post = new Post();
                    post.setCrawlUrl(url);
                    post.setStatus(Post.Status.UNCRAWL);
                    post.setCategory(source.getCategory());
                    postService.savePost(post);
                    articleLinks.add(url);
                }
            }

        } catch (IOException e) {
            System.err.println("Error crawling category: " + e.getMessage());
        }

        return articleLinks;
    }

    public void crawlUnCrawlPost(){
        List<Post> uncrawlPost = postService.getUnCrawlPosts(10);
        for (Post post : uncrawlPost) {
            String url = post.getCrawlUrl();
            try {
                Document doc = Jsoup.connect(url)
                        .timeout(5000)
                        .get();

                String title = doc.select("h1").text();
                String content = doc.select("article.fck_detail").text();
                String shortDescription = doc.select("p.description").text();
                post.setShortDescription(shortDescription);
                post.setTitle(title);
                post.setContent(content);
                post.setStatus(Post.Status.CRAWLED);
                postService.savePost(post);
            } catch (IOException e) {
                System.err.println("Error crawling category: " + e.getMessage());
            }

        }
    }
}
