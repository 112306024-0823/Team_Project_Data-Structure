package com.example.searchengine.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class CourseRanker {

    // 初始化關鍵字權重
    private static Map<String, Integer> loadKeywordWeights() {
        Map<String, Integer> keywordWeights = new HashMap<>();

        // 政大相關
        keywordWeights.put("政大", 150);
        keywordWeights.put("政治大學", 100);
        keywordWeights.put("NCCU", 100);

        // 課程名稱和評價相關
        keywordWeights.put("課程", 50);
        keywordWeights.put("評價", 30);
        keywordWeights.put("心得", 25);
        keywordWeights.put("推薦", 30);
        keywordWeights.put("必選", 20);
        keywordWeights.put("好過", 20);

        // 課程特性
        keywordWeights.put("甜", 40);
        keywordWeights.put("涼", 35);
        keywordWeights.put("簡單", 30);
        keywordWeights.put("挑戰性", 25);
        keywordWeights.put("加簽", 50);

        // 教學特點
        keywordWeights.put("老師", 40);
        keywordWeights.put("互動", 30);
        keywordWeights.put("討論", 25);
        keywordWeights.put("報告", 20);
        keywordWeights.put("授課", 20);
        keywordWeights.put("分組", 15);

        return keywordWeights;
    }

    private final Map<String, Integer> keywordWeights;

    public CourseRanker() {
        this.keywordWeights = loadKeywordWeights();
    }

    public Map<String, Integer> rankKeywords(Map<String, String> results, List<String> keywords) {
        if (results == null || keywords == null || keywords.isEmpty()) {
            return new HashMap<>(); // 返回空結果
        }

        Map<String, Integer> rankedResults = new HashMap<>();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String title = entry.getKey();
            String content = entry.getValue();

            // 標題匹配檢查
            if (containsAnyKeyword(title, keywords)) {
                int score = calculateKeywordScore(title, content, keywords);
                rankedResults.put(title, score);
            }
        }

        return rankedResults.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // 降序排序
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private int calculateKeywordScore(String title, String content, List<String> keywords) {
        int score = 0;

        // 遍歷關鍵字並統一計分
        for (String keyword : keywords) {
            if (title.contains(keyword)) {
                score += 5; // 標題關鍵字加分
            }
            score += content.split(Pattern.quote(keyword), -1).length - 1; // 內文關鍵字次數計算
        }

        // 關鍵字權重計分
        for (Map.Entry<String, Integer> entry : keywordWeights.entrySet()) {
            String evalKeyword = entry.getKey();
            int weight = entry.getValue();

            if (title.contains(evalKeyword)) {
                score += weight * 2; // 標題中的關鍵字權重更高
            }
            if (content.contains(evalKeyword)) {
                score += weight; // 內文中的關鍵字權重
            }
        }

        return score;
    }
}
