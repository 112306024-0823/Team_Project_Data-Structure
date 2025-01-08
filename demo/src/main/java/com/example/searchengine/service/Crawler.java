package com.example.searchengine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class Crawler {

    public String fetchContent(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(5000)
                            .get();
        return doc.html();
    }

    public List<String> extractParagraphs(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        List<String> paragraphs = new ArrayList<>();

        // 提取段落內容
        doc.select("p").forEach(paragraph -> paragraphs.add(paragraph.text()));

        return paragraphs;
    }

    public List<String> extractSubLinks(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        List<String> subLinks = new ArrayList<>();

        // 提取所有 <a> 標籤中的 href 屬性
        doc.select("a[href]").forEach(link -> {
            String href = link.attr("abs:href"); // 獲取絕對 URL
            if (href.startsWith("http")) { // 過濾掉無效的鏈接
                subLinks.add(href);
            }
        });

        return subLinks;
    }

    public Map<String, String> fetchAllContents(String url, int maxDepth) throws IOException {
        Map<String, String> contentMap = new HashMap<>();
        fetchRecursively(url, maxDepth, 0, contentMap);
        return contentMap;
    }

    private void fetchRecursively(String url, int maxDepth, int currentDepth, Map<String, String> contentMap) throws IOException {
        if (currentDepth > maxDepth) return; // 停止遞歸

        // 抓取當前網頁內容
        String content = fetchContent(url);
        contentMap.put(url, content);

        // 提取子鏈接
        List<String> subLinks = extractSubLinks(content);
        for (String subLink : subLinks) {
            if (!contentMap.containsKey(subLink)) { // 防止重複抓取
                fetchRecursively(subLink, maxDepth, currentDepth + 1, contentMap);
            }
        }
    }

    public List<String> filterParagraphs(List<String> paragraphs, List<String> stopWords) {
        return paragraphs.stream()
                .filter(paragraph -> stopWords.stream().noneMatch(paragraph::contains)) // 過濾含有停用詞的段落
                .toList();
    }
}
