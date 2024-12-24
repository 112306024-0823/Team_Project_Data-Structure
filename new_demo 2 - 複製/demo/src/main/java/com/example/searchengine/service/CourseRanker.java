package com.example.searchengine.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // 使用 RC 演算法進行多關鍵字匹配
        List<Integer> keywordHashes = keywords.stream().map(this::computeHash).collect(Collectors.toList());

        Map<String, Integer> rankedResults = new HashMap<>();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String title = entry.getKey();
            String content = entry.getValue();

            // 匹配標題和內容
            int titleScore = searchWithRC(title, keywords, keywordHashes);
            int contentScore = searchWithRC(content, keywords, keywordHashes);

            // 總得分計算
            int totalScore = titleScore * 2 + contentScore; // 標題匹配權重更高

            // 關鍵字權重計分
            totalScore += calculateWeightedScore(title, content);

            rankedResults.put(title, totalScore);
        }

        // 按分數降序排序
        return rankedResults.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
    }

    private int searchWithRC(String text, List<String> keywords, List<Integer> keywordHashes) {
        int score = 0;
        int textLength = text.length();

        for (int i = 0; i < keywords.size(); i++) {
            String keyword = keywords.get(i);
            int keywordLength = keyword.length();
            int keywordHash = keywordHashes.get(i);

            // 滑動窗口計算哈希值
            for (int j = 0; j <= textLength - keywordLength; j++) {
                String substring = text.substring(j, j + keywordLength);
                if (computeHash(substring) == keywordHash && substring.equals(keyword)) {
                    score += 1; // 匹配成功得分
                }
            }
        }

        return score;
    }

    private int computeHash(String str) {
        int hash = 0;
        int prime = 31; // 使用素数作为权重基数
        for (char c : str.toCharArray()) {
            hash = hash * prime + c;
        }
        return hash;
    }

    private int calculateWeightedScore(String title, String content) {
        int score = 0;
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
