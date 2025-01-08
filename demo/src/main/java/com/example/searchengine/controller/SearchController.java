// SearchController
package com.example.searchengine.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.searchengine.service.GoogleQuery;
import com.example.searchengine.service.KeywordExtractor;

@Controller
public class SearchController {

    @Autowired
    private GoogleQuery googleQuery;

    @Autowired
    private KeywordExtractor keywordExtractor;

    private List<String> globalKeywords = List.of("程式設計", "資料結構", "人工智慧", "演算法", "數據科學");

    @GetMapping("/search")
    public String search(@RequestParam(value = "query", required = false) String query,
                         @RequestParam(value = "courseType", required = false) String courseType,
                         @RequestParam(value = "courseYear", required = false) Integer year,
                         Model model) {

        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("error", "請輸入有效的搜尋關鍵字");
            model.addAttribute("suggestedKeywords", globalKeywords);
            return "index";
        }

        query = query.trim().toLowerCase(); // 標準化輸入

        try {
            Map<String, String> results = googleQuery.fetchResults(query, courseType, year);

            if (results == null || results.isEmpty()) {
                model.addAttribute("error", "未找到符合條件的搜尋結果: " + query);
                model.addAttribute("suggestedKeywords", globalKeywords);
            } else {
                model.addAttribute("results", results);
                List<String> suggestedKeywords = keywordExtractor.extractGlobalKeywords(results, 8);
                model.addAttribute("suggestedKeywords", suggestedKeywords.isEmpty() ? globalKeywords : suggestedKeywords);
            }

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "抓取搜尋結果時發生錯誤，請稍後再試。");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "系統發生異常，請聯絡管理員。");
        }

        model.addAttribute("query", query);
        model.addAttribute("courseType", courseType);
        model.addAttribute("courseYear", year);

        return "index";
    }
}