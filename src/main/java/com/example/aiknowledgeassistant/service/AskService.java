package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.entity.KnowledgeItem;
import com.example.aiknowledgeassistant.exception.ApiProblem;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AskService {
    private final KnowledgeService knowledge;
    private final AiGateway ai;
    private final Map<Long, Window> limits = new ConcurrentHashMap<>();
    private static class Window { final long start = System.currentTimeMillis(); int count; }
    public record Source(long id, String title, String excerpt) {}
    public record Answer(String mode, String answer, List<Source> sources, String warning) {}
    private record Ranked(KnowledgeItem item, int score) {}

    public AskService(KnowledgeService knowledge, AiGateway ai) { this.knowledge = knowledge; this.ai = ai; }
    public boolean aiEnabled() { return ai.enabled(); }

    private void limit(long ownerId) {
        long now = System.currentTimeMillis();
        limits.entrySet().removeIf(e -> now - e.getValue().start >= 60000);
        if (limits.size() >= 10000 && !limits.containsKey(ownerId)) throw new ApiProblem(429, "请求繁忙，请稍后重试");
        Window window = limits.computeIfAbsent(ownerId, unused -> new Window());
        synchronized (window) {
            if (++window.count > 10) throw new ApiProblem(429, "每分钟最多提问10次，请稍后重试");
        }
    }

    static List<String> keywords(String question) {
        Set<String> result = new LinkedHashSet<>();
        var matcher = Pattern.compile("[a-z0-9_]{2,}|[\\p{IsHan}]{2,}").matcher(question.toLowerCase(Locale.ROOT));
        Set<String> ignored = Set.of("什么", "如何", "怎么", "一个", "我的", "可以", "请问", "的是");
        while (matcher.find() && result.size() < 40) {
            String word = matcher.group();
            if (word.codePoints().allMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) {
                for (int i = 0; i < word.length() - 1 && result.size() < 40; i++) {
                    String pair = word.substring(i, i + 2);
                    if (!ignored.contains(pair)) result.add(pair);
                }
            } else result.add(word);
        }
        return List.copyOf(result);
    }

    private int score(KnowledgeItem item, List<String> words) {
        String title = item.getTitle().toLowerCase(Locale.ROOT);
        String text = item.getContent().toLowerCase(Locale.ROOT);
        String tags = item.getTags().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String word : words) {
            if (title.contains(word)) score += 4;
            if (tags.contains(word)) score += 3;
            if (text.contains(word)) score++;
        }
        return score;
    }

    public Answer ask(long ownerId, String question) {
        if (question == null || question.isBlank() || question.length() > 500) {
            throw new IllegalArgumentException("问题不能为空，且不能超过500字");
        }
        limit(ownerId);
        List<String> words = keywords(question);
        List<Source> sources = knowledge.recentForSearch(ownerId).stream()
                .map(item -> new Ranked(item, score(item, words))).filter(r -> r.score() > 0)
                .sorted(Comparator.comparingInt(Ranked::score).reversed())
                .limit(5).map(r -> new Source(r.item().getId(), r.item().getTitle(),
                        excerpt(r.item().getContent(), words))).toList();
        if (sources.isEmpty()) {
            return new Answer("retrieval", "没有找到相关资料。请先保存知识条目，或用标题中的关键词重新提问。",
                    sources, null);
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            Source source = sources.get(i);
            context.append("[").append(i + 1).append("] ").append(source.title()).append("\n")
                    .append(source.excerpt()).append("\n\n");
        }
        if (ai.enabled()) {
            try { return new Answer("ai", ai.answer(question.strip(), context.toString()), sources, null); }
            catch (IllegalStateException error) {
                return new Answer("retrieval", "AI服务暂不可用，以下是检索到的原文片段：\n\n" + context,
                        sources, "请检查模型配置或稍后重试");
            }
        }
        return new Answer("retrieval", "已从你的知识库找到以下资料（原文检索，非AI生成）：\n\n" + context, sources, null);
    }

    private String excerpt(String content, List<String> words) {
        String lower = content.toLowerCase(Locale.ROOT);
        int first = words.stream().mapToInt(lower::indexOf).filter(i -> i >= 0).min().orElse(0);
        int start = Math.max(0, first - 100), end = Math.min(content.length(), start + 1400);
        return (start > 0 ? "…" : "") + content.substring(start, end) + (end < content.length() ? "…" : "");
    }
}
