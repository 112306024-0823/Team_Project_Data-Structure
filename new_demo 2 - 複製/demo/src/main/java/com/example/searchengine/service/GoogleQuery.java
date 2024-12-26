package com.example.searchengine.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleQuery {

    @Autowired
    private Crawler crawler;

    @Autowired
    private HTMLHandler htmlHandler;

    public Map<String, String> fetchResults(String query, String courseType, Integer year) throws IOException {
        StringBuilder searchQuery = new StringBuilder(query);

        // 如果有課程類型，將其加入查詢字串
        if (courseType != null && !courseType.isEmpty()) {
            searchQuery.append(" ").append(courseType);
        }

        // 如果有年份，將其加入查詢字串
        if (year != null) {
            searchQuery.append(" ").append(year);
        }

        // 定義搜尋的目標網站
        String[] targetSites = {
            "dcard.tw/f/nccu",
            "qrysub.nccu.edu.tw",
            "1111opt.com",
            "medium.com",
            "ptt.cc/bbs/NCCU",
            "site:medium.com 政大"
        };

        // 合併目標網站作為查詢條件
        StringBuilder siteFilter = new StringBuilder();
        for (String site : targetSites) {
            siteFilter.append("site:").append(site).append(" OR ");
        }
        siteFilter.delete(siteFilter.length() - 4, siteFilter.length()); // 移除多餘的 " OR "

        // 將目標網站與用戶的關鍵字組合
        String url = "https://www.google.com/search?q=" + URLEncoder.encode("(" + siteFilter + ") " + searchQuery, "UTF-8") + "&num=50";
        System.out.println("Query URL: " + url);

        // 抓取搜尋結果
        String content = crawler.fetchContent(url);
        if (content == null || content.isEmpty()) {
            System.err.println("Failed to fetch content for URL: " + url);
            return new HashMap<>();
        }

        // 解析搜尋結果
        return htmlHandler.parseResults(content);
    }
}