package com.example.searchengine.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.searchengine.service.GoogleQuery;

@Controller
public class SearchController {

    @Autowired
    private GoogleQuery googleQuery;

    @GetMapping("/search")
    public String search(@RequestParam(value = "query", required = false) String query,
                         @RequestParam(value = "year", required = false) Integer year,
                         Model model) {
        // 若 query 為空，設置默認關鍵字
        if (query == null || query.isEmpty()) {
            query = "Default Keyword";
        }

        try {
            // 獲取搜尋結果，若有年份篩選，進行篩選處理
            Map<String, String> results = googleQuery.fetchResults(query, year);

            if (results == null || results.isEmpty()) {
                model.addAttribute("error", "No results found for query: " + query);
            } else {
                model.addAttribute("results", results);
            }
        } catch (IOException e) {
            model.addAttribute("error", "Error fetching results: " + e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error occurred: " + e.getMessage());
        }

        model.addAttribute("query", query);
        model.addAttribute("year", year);
        return "index"; // 返回 Thymeleaf 渲染的模板
    }
}