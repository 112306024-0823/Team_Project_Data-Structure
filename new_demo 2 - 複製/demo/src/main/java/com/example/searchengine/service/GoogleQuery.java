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

public Map<String, String> fetchResults(String query) throws IOException {
    // 定義搜尋的目標網站
    String[] targetSites = {
    "dcard.tw/f/nccu",                  // Dcard 的 NCCU 板
    "qrysub.nccu.edu.tw",               // 政大全校課程查詢系統
    "1111opt.com",    // OPT 與課程相關的分享頁
    "medium.com",              // Medium 上標籤為「政大」的頁面
    "ptt.cc/bbs/NCCU",              // PTT 政大板
    "site:medium.com 政大"

};


    // 合併目標網站作為查詢條件
    StringBuilder siteFilter = new StringBuilder();
    for (String site : targetSites) {
        siteFilter.append("site:").append(site).append(" OR ");
    }
    siteFilter.delete(siteFilter.length() - 4, siteFilter.length()); // 移除多餘的 " OR "

    // 將目標網站與用戶的關鍵字組合
    String url = "https://www.google.com/search?q=" + URLEncoder.encode("(" + siteFilter + ") " + query, "UTF-8") + "&num=100";
    System.out.println("Query URL: " + url);

    // 抓取搜尋結果
    String content = crawler.fetchContent(url);
    if (content == null || content.isEmpty()) {
        System.err.println("Failed to fetch content for URL: " + url);
        return new HashMap<>();
    }

    // 解析搜尋結果
    Map<String, String> results = htmlHandler.parseResults(content);
    if (results.isEmpty()) {
        System.err.println("No results parsed from content.");
    }

    return results;
}
}