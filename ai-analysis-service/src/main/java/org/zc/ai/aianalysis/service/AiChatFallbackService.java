package org.zc.ai.aianalysis.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiChatFallbackService {

    public String fallbackAnswer(
        AiIntent intent,
        String userMessage,
        List<KnowledgeSnippet> knowledgeSnippets,
        List<LogSnippet> logSnippets
    ) {
        StringBuilder builder = new StringBuilder("LLM service is temporarily unavailable. Returning a rule-based fallback.");
        if (intent == AiIntent.KAFKA_METRICS) {
            builder.append(" Kafka metric request was parsed, but deep analysis is unavailable right now.");
            builder.append(" Please retry later or provide explicit metric/topic/group details for a narrower query.");
        } else if (intent == AiIntent.OPERATIONS_QA) {
            builder.append(" Current evidence suggests checking the latest logs and related runbooks first.");
        } else {
            builder.append(" Please restate the issue with concrete identifiers such as topic, group, traceId, time window, or failing component.");
        }

        appendLogs(builder, logSnippets);
        appendKnowledge(builder, knowledgeSnippets);

        if (!StringUtils.hasText(userMessage)) {
            return builder.toString();
        }
        builder.append(" Original request: ").append(userMessage.trim());
        return builder.toString();
    }

    private void appendLogs(StringBuilder builder, List<LogSnippet> logSnippets) {
        if (logSnippets == null || logSnippets.isEmpty()) {
            return;
        }
        String evidence = logSnippets.stream()
            .limit(2)
            .map(LogSnippet::summary)
            .collect(Collectors.joining(" | "));
        if (StringUtils.hasText(evidence)) {
            builder.append(" Relevant logs: ").append(evidence).append(".");
        }
    }

    private void appendKnowledge(StringBuilder builder, List<KnowledgeSnippet> knowledgeSnippets) {
        if (knowledgeSnippets == null || knowledgeSnippets.isEmpty()) {
            return;
        }
        String references = knowledgeSnippets.stream()
            .limit(2)
            .map(KnowledgeSnippet::title)
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(", "));
        if (StringUtils.hasText(references)) {
            builder.append(" Related runbooks: ").append(references).append(".");
        }
    }
}
