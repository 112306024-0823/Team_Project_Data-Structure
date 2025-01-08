package com.example.searchengine.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;


@Service
public class HTMLHandler {
    
    public List<String> extractSubLinks(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Elements links = doc.select("a[href]"); // 提取所有<a>標籤的 href 屬性

        return links.stream()
                .map(link -> link.attr("abs:href")) // 獲取絕對 URL
                .filter(url -> url.startsWith("http")) // 過濾有效的 HTTP URL
                .distinct() // 去重
                .collect(Collectors.toList());
    }


    public Map<String, String> parseResults(String htmlContent) {
        Map<String, String> results = new HashMap<>();
        Document doc = Jsoup.parse(htmlContent);

        Elements elements = doc.select("div.yuRUbf a");
        if (elements.isEmpty()) {
            System.err.println("No elements found for the provided HTML content.");
            return results;
        }

        for (Element element : elements) {
            try {
                String url = element.attr("href").replace("/url?q=", "");
                if (url.contains("&")) {
                    url = url.split("&")[0]; // 去除不必要的參數
                }
                String title = element.select("h3").text(); // 更新选择器获取标题

                if (!title.isEmpty()) {
                    results.put(title, url);
                }
            } catch (Exception e) {
                System.err.println("Error parsing element: " + element);
                e.printStackTrace();
            }
        }

        return results;
    }
}