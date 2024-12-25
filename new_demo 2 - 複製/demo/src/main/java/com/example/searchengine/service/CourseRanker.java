package com.example.searchengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class CourseRanker {

    // 初始化關鍵字權重
    private static Map<String, Integer> loadKeywordWeights() {
        Map<String, Integer> keywordWeights = new HashMap<>();

        // 政大相關
        keywordWeights.put("政大", 150);
        //keywordWeights.put("政治大學", 100);
        //keywordWeights.put("NCCU", 100);

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

        // 初始化 Aho-Corasick 自動機
        ACAutomaton automaton = new ACAutomaton();
        for (String keyword : keywords) {
            automaton.insert(keyword);
        }
        automaton.buildFailureLinks();

        Map<String, Integer> rankedResults = new HashMap<>();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            String title = entry.getKey();
            String content = entry.getValue();

            // 跳過包含特定關鍵字的標題
            if (title.contains("考古題") || title.contains("加簽")) {
                continue;
            }



            // 匹配標題和內容
            Map<String, Integer> titleMatches = automaton.search(title);
            Map<String, Integer> contentMatches = automaton.search(content);

            // 計算總得分
            int totalScore = calculateACScore(titleMatches, contentMatches);
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

    private int calculateACScore(Map<String, Integer> titleMatches, Map<String, Integer> contentMatches) {
        int score = 0;

        // 標題和內容匹配的加權分數
        for (Map.Entry<String, Integer> entry : titleMatches.entrySet()) {
            score += entry.getValue() * 5; // 標題匹配得分更高
        }

        for (Map.Entry<String, Integer> entry : contentMatches.entrySet()) {
            score += entry.getValue(); // 內容匹配得分
        }

        return score;
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

    // Aho-Corasick 自動機實現
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