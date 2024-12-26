package com.example.searchengine.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class KeywordExtractor {

    // 停用詞清單（可擴展）
    private static final Set<String> STOP_WORDS = Set.of("的", "是", "在", "和", "也", "對", "於", "了", "有", "啊");

    /**
     * 從單篇文本中提取關鍵字及其頻率
     *
     * @param text 文本（標題或內文）
     * @return 關鍵字清單及其出現次數
     */
    public Map<String, Integer> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyMap();
        }

        // 正則表達式匹配中文字詞
        Pattern pattern = Pattern.compile("[\\p{IsHan}]+");
        Matcher matcher = pattern.matcher(text);

        Map<String, Integer> keywordFrequency = new HashMap<>();

        while (matcher.find()) {
            String word = matcher.group();

            // 過濾停用詞
            if (!STOP_WORDS.contains(word) && word.length() > 1) {
                keywordFrequency.put(word, keywordFrequency.getOrDefault(word, 0) + 1);
            }
        }

        return keywordFrequency;
    }

    /**
     * 合併多篇文本的關鍵字頻率
     *
     * @param titles  標題集合
     * @param content 內文集合
     * @return 全局的關鍵字頻率圖
     */
    public Map<String, Integer> mergeKeywordFrequencies(List<String> titles, List<String> content) {
        Map<String, Integer> combinedFrequency = new HashMap<>();

        // 提取並合併標題的關鍵字
        for (String title : titles) {
            Map<String, Integer> titleKeywords = extractKeywords(title);
            titleKeywords.forEach((key, value) ->
                    combinedFrequency.put(key, combinedFrequency.getOrDefault(key, 0) + value * 2)); // 標題權重大
        }

        // 提取並合併內文的關鍵字
        for (String paragraph : content) {
            Map<String, Integer> contentKeywords = extractKeywords(paragraph);
            contentKeywords.forEach((key, value) ->
                    combinedFrequency.put(key, combinedFrequency.getOrDefault(key, 0) + value)); // 內文權重正常
        }

        return combinedFrequency;
    }

    /**
     * 提取並排序多篇文本中的前 N 個高頻關鍵字
     *
     * @param titles  標題集合
     * @param content 內文集合
     * @param limit   限制返回的關鍵字數量
     * @return 排序後的關鍵字清單
     */
    public List<String> rankCombinedKeywords(List<String> titles, List<String> content, int limit) {
        Map<String, Integer> combinedFrequency = mergeKeywordFrequencies(titles, content);

        // 按頻率排序並返回前 N 個關鍵字
        return combinedFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 計算單篇文本的總分
     *
     * @param text           文本（標題或內文）
     * @param keywordWeights 關鍵字權重表
     * @return 該文本的加權總分
     */
    public int calculateScore(String text, Map<String, Integer> keywordWeights) {
        Map<String, Integer> extractedKeywords = extractKeywords(text);
        int score = 0;

        for (Map.Entry<String, Integer> entry : extractedKeywords.entrySet()) {
            String keyword = entry.getKey();
            int frequency = entry.getValue();
            int weight = keywordWeights.getOrDefault(keyword, 0);
            score += frequency * weight; // 計算每個關鍵字的加權分數
        }

        return score;
    }

    /**
     * 提取全局推薦的關鍵字（按頻率排序）
     *
     * @param results 包含標題和內文的結果集合
     * @param limit   限制返回的關鍵字數量
     * @return 排序後的推薦關鍵字
     */
    public List<String> extractGlobalKeywords(Map<String, String> results, int limit) {
        Map<String, Integer> globalKeywordFrequency = new HashMap<>();

        for (Map.Entry<String, String> entry : results.entrySet()) {
            String title = entry.getKey();
            String content = entry.getValue();

            // 提取並合併標題的關鍵字
            Map<String, Integer> titleKeywords = extractKeywords(title);
            titleKeywords.forEach((key, value) ->
                    globalKeywordFrequency.put(key, globalKeywordFrequency.getOrDefault(key, 0) + value * 2)); // 標題權重大

            // 提取並合併內文的關鍵字
            Map<String, Integer> contentKeywords = extractKeywords(content);
            contentKeywords.forEach((key, value) ->
                    globalKeywordFrequency.put(key, globalKeywordFrequency.getOrDefault(key, 0) + value)); // 內文權重正常
        }

        // 排序並返回前 N 個高頻關鍵字
        return globalKeywordFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }
}
