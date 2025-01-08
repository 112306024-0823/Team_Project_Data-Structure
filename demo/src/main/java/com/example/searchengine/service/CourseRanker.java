package com.example.searchengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
        keywordWeights.put("政大", 200);
        keywordWeights.put("政治大學", 50);
        keywordWeights.put("NCCU", 200);

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

      // 提供權重表的 getter
    public Map<String, Integer> getKeywordWeights() {
        return keywordWeights;
    }

    // 使用關鍵字提取與分數計算進行排名
        public Map<String, Integer> rankResults(Map<String, String> mainPageResults, Map<String, List<String>> subPageLinks) {
        Map<String, Integer> rankedResults = new HashMap<>();

        for (Map.Entry<String, String> entry : mainPageResults.entrySet()) {
            String mainPageTitle = entry.getKey();
            String mainPageContent = entry.getValue();

            // 計算主網頁分數
            int mainPageScore = calculatePageScore(mainPageTitle, mainPageContent);

            // 計算子網頁總分
            List<String> subPages = subPageLinks.getOrDefault(mainPageTitle, new ArrayList<>());
            int subPagesScore = calculateSubPagesScore(subPages);

            // 合併主網頁與子網頁分數
            int totalScore = (int) (mainPageScore * 0.7 + subPagesScore * 0.3);
            rankedResults.put(mainPageTitle, totalScore);
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

    // 計算單頁分數（主網頁或子網頁）
    private int calculatePageScore(String title, String content) {
        int titleScore = keywordExtractor.calculateScore(title, keywordWeights) * 2; // 標題分數 ×2
        int contentScore = keywordExtractor.calculateScore(content, keywordWeights);

        // 使用 AC 匹配計分
        Map<String, Integer> titleMatches = matchKeywordsUsingAC(title);
        Map<String, Integer> contentMatches = matchKeywordsUsingAC(content);
        int acScore = calculateACScore(titleMatches, contentMatches);

        return titleScore + contentScore + acScore;
    }

    // 計算所有子網頁的總分
    private int calculateSubPagesScore(List<String> subPages) {
        int totalScore = 0;
        for (String subPageContent : subPages) {
            totalScore += keywordExtractor.calculateScore(subPageContent, keywordWeights);
        }
        return totalScore;
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
