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

        // 組裝 Google 搜尋 URL
        String url = "https://www.google.com/search?q=" + URLEncoder.encode("(" + siteFilter + ") " + searchQuery, "UTF-8") + "&num=150";
        System.out.println("Query URL: " + url);

        // 抓取主頁內容
        String mainPageContent = crawler.fetchContent(url);
        if (mainPageContent == null || mainPageContent.isEmpty()) {
            System.err.println("Failed to fetch content for URL: " + url);
            return new HashMap<>();
        }

        // 解析主頁結果
        Map<String, String> results = htmlHandler.parseResults(mainPageContent);

        // 提取子網頁鏈接
        List<String> subLinks = htmlHandler.extractSubLinks(mainPageContent);

        // 抓取子網頁內容
        for (String subLink : subLinks) {
            try {
                String subContent = crawler.fetchContent(subLink);
                results.put("SubPage: " + subLink, subContent); // 標記為子網頁
            } catch (IOException e) {
                System.err.println("Failed to fetch subpage content: " + subLink);
            }
        }

        return results; // 返回主頁與子頁的綜合結果
    }
}
