package com.example.searchengine.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleQuery {

    @Autowired
    private Crawler crawler;

    @Autowired
    private HTMLHandler htmlHandler;

    @Autowired
    private CourseRanker courseRanker;

    @Autowired
    private KeywordExtractor keywordExtractor;

    public Map<String, String> fetchResults(String query, String courseType, Integer year) throws IOException {
        StringBuilder searchQuery = new StringBuilder(query);

        // 如果有課程類型和年份，加入查詢字串
        if (courseType != null && !courseType.isEmpty()) searchQuery.append(" ").append(courseType);
        if (year != null) searchQuery.append(" ").append(year);

        // 搜尋的目標網站
        String[] targetSites = {
            "dcard.tw/f/nccu",
            "qrysub.nccu.edu.tw",
            "1111opt.com",
            "ptt.cc/bbs/NCCU"
            
        };

        StringBuilder siteFilter = new StringBuilder();
        for (String site : targetSites) siteFilter.append("site:").append(site).append(" OR ");
        siteFilter.delete(siteFilter.length() - 4, siteFilter.length());

        String url = "https://www.google.com/search?q=" + URLEncoder.encode("(" + siteFilter + ") " + searchQuery, "UTF-8") + "&num=20";
        System.out.println("Query URL: " + url);

        String mainPageContent = crawler.fetchContent(url);
        if (mainPageContent == null || mainPageContent.isEmpty()) {
            System.err.println("Failed to fetch content for URL: " + url);
            return new HashMap<>();
        }

        // 解析主頁結果
        Map<String, String> results = htmlHandler.parseResults(mainPageContent);

        // 處理子頁面內容
        List<String> subLinks = htmlHandler.extractSubLinks(mainPageContent);
        for (String subLink : subLinks) {
            try {
                String subContent = crawler.fetchContent(subLink);
                if (subContent != null && !subContent.isEmpty()) {
                    int additionalScore = calculateSubPageScore(subContent);
                    updateMainPageScore(results, subLink, additionalScore);
                }
            } catch (IOException e) {
                System.err.println("Failed to fetch subpage content: " + subLink);
            }
        }

        return results; // 僅返回主頁結果
    }

    private int calculateSubPageScore(String subContent) {
        return keywordExtractor.calculateScore(subContent, courseRanker.getKeywordWeights());
    }
    private void updateMainPageScore(Map<String, String> results, String subLink, int additionalScore) {
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String mainPageTitle = entry.getKey();
            if (subLink.contains(mainPageTitle)) {
                int currentScore = Integer.parseInt(entry.getValue()); // 假設 value 存的是分數
                int updatedScore = currentScore + additionalScore; // 合併分數
                entry.setValue(String.valueOf(updatedScore)); // 更新分數

                System.out.println("Updating score for main page: " + mainPageTitle);
               
            }
        }
    }
}
