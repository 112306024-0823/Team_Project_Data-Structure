package com.example.searchengine.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HTMLHandler {

    private static final Logger logger = LoggerFactory.getLogger(HTMLHandler.class);
    private static final Pattern VALID_URL_PATTERN = Pattern.compile("^(http|https)://.*$");

public Map<String, String> parseResults(String htmlContent) {
    Map<String, String> results = new HashMap<>();
    try {
        Document doc = Jsoup.parse(htmlContent);

        // 使用 Google 搜尋結果常見選擇器
        Elements elements = doc.select("div.yuRUbf a");

        if (elements.isEmpty()) {
            logger.warn("No elements found for the provided HTML content.");
            return results;
        }

        for (Element element : elements) {
            try {
                // 提取 URL
                String url = element.attr("href").replace("/url?q=", "");
                if (url.contains("&")) {
                    url = url.split("&")[0]; // 去除多餘參數
                }

                // 驗證 URL 格式
                if (!url.startsWith("http")) {
                    logger.warn("Invalid URL detected: {}", url);
                    continue;
                }

                // 提取標題
                String title = element.select("h3").text();
                if (title.isEmpty()) {
                    logger.warn("Empty title for URL: {}", url);
                    continue;
                }

                results.put(title, url);
            } catch (Exception e) {
                logger.error("Error parsing element: {}", element, e);
            }
        }

        // 排序結果
        results = results.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                          (e1, e2) -> e1, LinkedHashMap::new));
    } catch (Exception e) {
        logger.error("Error parsing HTML content.", e);
    }

    return results;
}
}
