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
                         @RequestParam(value = "courseType", required = false) String courseType,
                         @RequestParam(value = "courseYear", required = false) Integer year,
                         Model model) {
        // 檢查查詢參數是否為空
        if (query == null || query.isEmpty()) {
            model.addAttribute("error", "請輸入搜尋關鍵字");
            return "index"; // 返回到主頁面
        }

        try {
            // 呼叫 GoogleQuery 獲取搜尋結果
            Map<String, String> results = googleQuery.fetchResults(query, courseType, year);

            // 如果結果為空，顯示提示
            if (results == null || results.isEmpty()) {
                model.addAttribute("error", "未找到符合條件的搜尋結果: " + query);
            } else {
                model.addAttribute("results", results); // 將結果傳遞給模板
            }
        } catch (IOException e) {
            // 處理 GoogleQuery 中的 IOException
            model.addAttribute("error", "抓取搜尋結果時發生錯誤: " + e.getMessage());
        } catch (Exception e) {
            // 處理其他未捕捉的異常
            model.addAttribute("error", "系統發生異常: " + e.getMessage());
        }

        // 將用戶查詢條件傳遞到前端
        model.addAttribute("query", query);
        model.addAttribute("courseType", courseType);
        model.addAttribute("courseYear", year);

        return "index"; // 返回 Thymeleaf 模板名稱
    }
}
