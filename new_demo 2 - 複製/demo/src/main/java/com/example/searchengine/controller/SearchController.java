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
public String search(@RequestParam(value = "query", required = false) String query, Model model) {
    if (query == null || query.isEmpty()) {
        model.addAttribute("error", "Query cannot be empty");
        return "index"; // 返回 index.html
    }

    try {
        // 獲取搜尋結果
        Map<String, String> results = googleQuery.fetchResults(query);

        if (results == null || results.isEmpty()) {
            model.addAttribute("error", "No results found for query: " + query);
        } else {
            model.addAttribute("results", results);
        }
    } catch (IOException e) { // 捕捉 fetchResults 方法中可能的 IOException
        model.addAttribute("error", "Error fetching results: " + e.getMessage());
    } catch (Exception e) { // 捕捉其他可能的異常
        model.addAttribute("error", "Unexpected error occurred: " + e.getMessage());
    }

    model.addAttribute("query", query);
    return "index"; // Thymeleaf 渲染的模板
}

}
