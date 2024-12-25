package com.example.searchengine.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.searchengine.service.CourseRanker;
import com.example.searchengine.service.GoogleQuery;


@Controller
public class SearchController {

    @Autowired
    private GoogleQuery googleQuery;

    @Autowired
    private CourseRanker courseRanker;

    @GetMapping("/search")
    public String search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "courseType", required = false) String courseType,
            @RequestParam(value = "courseYear", required = false) Integer year,
            Model model) {
        // 如果查詢參數為空，設置預設值
        if (query == null || query.isEmpty()) {
            query = "Default Keyword";
        }

        try {
            // 呼叫 GoogleQuery 獲取搜尋結果
            Map<String, String> results = googleQuery.fetchResults(query, courseType, year);

            if (results.isEmpty()) {
                model.addAttribute("error", "No results found for query: " + query);
            } else {
                // 將關鍵字拆解並傳給 CourseRanker 進行排序
                Map<String, Integer> rankedResults = courseRanker.rankKeywords(results, List.of(query.split(" ")), courseType, year);
                model.addAttribute("results", rankedResults);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unexpected error occurred: " + e.getMessage());
        }

        // 將查詢參數加入到模型中
        model.addAttribute("query", query);
        model.addAttribute("courseType", courseType);
        model.addAttribute("courseYear", year);

        return "index"; // 返回前端模板名稱
    }
}
