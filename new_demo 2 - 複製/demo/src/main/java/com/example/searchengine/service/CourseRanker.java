package com.example.searchengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseRanker {

    private final Map<String, Integer> keywordWeights;

    @Autowired
    private KeywordExtractor keywordExtractor;

    public CourseRanker() {
        this.keywordWeights = loadKeywordWeights();
    }

    // 初始化關鍵字權重
    private static Map<String, Integer> loadKeywordWeights() {
        Map<String, Integer> keywordWeights = new HashMap<>();

        // 政大相關
        keywordWeights.put("政大", 150);
        keywordWeights.put("政治大學", 50);


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
        keywordWeights.put("加簽", 20);

        // 教學特點
        keywordWeights.put("老師", 40);
        keywordWeights.put("互動", 30);
        keywordWeights.put("討論", 25);
        keywordWeights.put("報告", 20);
        keywordWeights.put("授課", 20);
        keywordWeights.put("分組", 15);

        return keywordWeights;
    }

    // 使用關鍵字提取與分數計算進行排名
    public Map<String, Integer> rankKeywords(Map<String, String> results) {
        if (results == null || results.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Integer> rankedResults = new HashMap<>();
        // 定義需要過濾的標題或內文中的關鍵字
        Set<String> ignoredPhrases = Set.of("作弊", "研究所", "考古題","台大","轉行","加簽","北一女");


        for (Map.Entry<String, String> entry : results.entrySet()) {
            String title = entry.getKey();
            String content = entry.getValue();

            // 過濾標題或內文中包含不需要的關鍵字
            if (ignoredPhrases.stream().anyMatch(title::contains) || ignoredPhrases.stream().anyMatch(content::contains)) {
                continue; // 跳過這條記錄
            }


            // 使用 KeywordExtractor 提取並計算分數
            int extractedTitleScore = keywordExtractor.calculateScore(title, keywordWeights);
            int extractedContentScore = keywordExtractor.calculateScore(content, keywordWeights);

            // 使用 Aho-Corasick 匹配算法
            Map<String, Integer> titleMatches = matchKeywordsUsingAC(title);
            Map<String, Integer> contentMatches = matchKeywordsUsingAC(content);
            int acScore = calculateACScore(titleMatches, contentMatches);

            // 總分計算
            int totalScore = extractedTitleScore * 2 + extractedContentScore + acScore;

            // 存入排名結果
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

    private Map<String, Integer> matchKeywordsUsingAC(String text) {
        ACAutomaton automaton = new ACAutomaton();
        for (String keyword : keywordWeights.keySet()) {
            automaton.insert(keyword);
        }
        automaton.buildFailureLinks();
        return automaton.search(text);
    }

    private int calculateACScore(Map<String, Integer> titleMatches, Map<String, Integer> contentMatches) {
        int score = 0;

        // 標題和內文匹配的加權分數
        for (Map.Entry<String, Integer> entry : titleMatches.entrySet()) {
            score += entry.getValue() * 5; // 標題匹配得分更高
        }

        for (Map.Entry<String, Integer> entry : contentMatches.entrySet()) {
            score += entry.getValue(); // 內文匹配得分
        }

        return score;
    }

    // Aho-Corasick 自動機實現（保留原有邏輯）
    private static class ACAutomaton {
        private final TrieNode root;

        public ACAutomaton() {
            root = new TrieNode();
        }

        public void insert(String keyword) {
            TrieNode node = root;
            for (char c : keyword.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new TrieNode());
            }
            node.outputs.add(keyword);
        }

        public void buildFailureLinks() {
            Queue<TrieNode> queue = new LinkedList<>();
            root.fail = root;

            for (TrieNode child : root.children.values()) {
                child.fail = root;
                queue.add(child);
            }

            while (!queue.isEmpty()) {
                TrieNode current = queue.poll();

                for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                    char c = entry.getKey();
                    TrieNode child = entry.getValue();

                    TrieNode failNode = current.fail;
                    while (failNode != root && !failNode.children.containsKey(c)) {
                        failNode = failNode.fail;
                    }

                    if (failNode.children.containsKey(c)) {
                        child.fail = failNode.children.get(c);
                    } else {
                        child.fail = root;
                    }

                    child.outputs.addAll(child.fail.outputs);
                    queue.add(child);
                }
            }
        }

        public Map<String, Integer> search(String text) {
            Map<String, Integer> keywordCounts = new HashMap<>();
            TrieNode node = root;

            for (char c : text.toCharArray()) {
                while (node != root && !node.children.containsKey(c)) {
                    node = node.fail;
                }

                if (node.children.containsKey(c)) {
                    node = node.children.get(c);
                }

                for (String keyword : node.outputs) {
                    keywordCounts.put(keyword, keywordCounts.getOrDefault(keyword, 0) + 1);
                }
            }

            return keywordCounts;
        }

        private static class TrieNode {
            Map<Character, TrieNode> children = new HashMap<>();
            TrieNode fail;
            List<String> outputs = new ArrayList<>();
        }
    }
}
