package org.zc.ai.aianalysis.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Service
public class QuickReplyService {

    private static final Set<String> HEALTH_REQUESTS = Set.of("ping", "health", "status", "状态", "健康");
    private static final Set<String> HELP_REQUESTS = Set.of("help", "帮助", "你能做什么", "what can you do", "how to use", "怎么用");

    public boolean supports(String message) {
        String normalized = normalize(message);
        return HEALTH_REQUESTS.contains(normalized) || HELP_REQUESTS.contains(normalized);
    }

    public String reply(String message) {
        String normalized = normalize(message);
        if (HEALTH_REQUESTS.contains(normalized)) {
            return "AI analysis service is available. Ask about Kafka metrics, production logs, or troubleshooting guidance.";
        }
        return "I can help with Kafka metrics, Elasticsearch log troubleshooting, and runbook-based operations Q&A.";
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .trim();
    }
}
