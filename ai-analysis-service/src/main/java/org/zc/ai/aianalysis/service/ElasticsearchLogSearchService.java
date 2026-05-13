package org.zc.ai.aianalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.zc.ai.aianalysis.config.AiOpsProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * Elasticsearch 日志检索服务。
 * 仅在问题明显偏排障场景，或显式指定日志工具时才触发检索。
 */
public class ElasticsearchLogSearchService {

    private static final List<String> LOG_HINTS = List.of("error", "exception", "fail", "timeout", "告警", "异常", "失败", "超时");

    private final AiOpsProperties properties;
    private final ObjectMapper objectMapper;

    public List<LogSnippet> search(String message, String preferredTool) {
        if (!properties.getTools().isLogSearchEnabled()
            || !properties.getElasticsearch().isEnabled()
            || !shouldSearch(message, preferredTool)) {
            return List.of();
        }
        try {
            // 这里直接走 ES REST 检索，避免日志查询路径和 RAG 的向量实现强耦合。
            RestClient client = RestClient.builder()
                .baseUrl(properties.getElasticsearch().getUrl())
                .build();
            String endpoint = "/" + properties.getElasticsearch().getLogsIndexPattern() + "/_search";
            String body = objectMapper.writeValueAsString(Map.of(
                "size", properties.getElasticsearch().getLogSize(),
                "query", Map.of("multi_match", Map.of(
                    "query", message,
                    "fields", List.of("message", "summary", "stacktrace", "traceId")
                ))
            ));
            String response = client.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
            return parse(response);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public String formatForPrompt(List<LogSnippet> snippets) {
        if (snippets.isEmpty()) {
            return "No log context available.";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < snippets.size(); i++) {
            LogSnippet snippet = snippets.get(i);
            builder.append(i + 1)
                .append(". [")
                .append(snippet.index())
                .append("] ")
                .append(snippet.summary())
                .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private boolean shouldSearch(String message, String preferredTool) {
        if ("logSearchTool".equalsIgnoreCase(preferredTool)) {
            return true;
        }
        String lowerCase = message == null ? "" : message.toLowerCase();
        return LOG_HINTS.stream().anyMatch(lowerCase::contains);
    }

    private List<LogSnippet> parse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode hits = root.path("hits").path("hits");
        List<LogSnippet> snippets = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            snippets.add(new LogSnippet(
                hit.path("_id").asText(),
                hit.path("_index").asText(),
                firstNonBlank(
                    source.path("summary").asText(null),
                    source.path("message").asText(null),
                    source.toString()
                )
            ));
        }
        return snippets;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }
}
