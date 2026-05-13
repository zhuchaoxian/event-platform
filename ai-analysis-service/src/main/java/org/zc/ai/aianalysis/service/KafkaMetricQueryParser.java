package org.zc.ai.aianalysis.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.dto.RepairSuggestion;
import org.zc.ai.aianalysis.tool.KafkaMetricQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
/**
 * 从自然语言或 key=value 风格输入中提取 Kafka 指标查询参数，
 * 并输出结构化修复建议。
 */
public class KafkaMetricQueryParser {

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(metric|topic|group|broker|cluster|window|指标|主题|消费组|时间窗)\\s*[:=：]\\s*([^\\s,，]+)");

    private final AiOpsProperties properties;

    public KafkaMetricQueryParser(AiOpsProperties properties) {
        this.properties = properties;
    }

    public KafkaMetricQuery parse(String message) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(message);
        while (matcher.find()) {
            values.put(normalizeKey(matcher.group(1)), matcher.group(2));
        }

        String lowerCase = message == null ? "" : message.toLowerCase(Locale.ROOT);
        String metric = values.get("metric");
        if (!StringUtils.hasText(metric)) {
            metric = inferMetric(lowerCase);
        }

        return KafkaMetricQuery.builder()
            .metric(metric)
            .topic(values.get("topic"))
            .group(values.get("group"))
            .broker(values.get("broker"))
            .cluster(values.get("cluster"))
            .window(StringUtils.hasText(values.get("window")) ? values.get("window") : properties.getPrometheus().getDefaultWindow())
            .rawInput(message)
            .build();
    }

    public RepairSuggestion validate(KafkaMetricQuery query) {
        if (!StringUtils.hasText(query.getMetric())) {
            return RepairSuggestion.builder()
                .code("MISSING_METRIC")
                .message("Kafka metrics request is missing metric. Supported metrics: " + supportedMetrics())
                .missingFields(List.of("metric"))
                .exampleInput("metric=lag topic=orders group=order-service")
                .build();
        }

        AiOpsProperties.MetricDefinition definition = properties.getMetrics().getDefinitions().get(query.getMetric());
        if (definition == null) {
            return RepairSuggestion.builder()
                .code("UNSUPPORTED_METRIC")
                .message("Unsupported metric '" + query.getMetric() + "'. Supported metrics: " + supportedMetrics())
                .missingFields(List.of())
                .exampleInput("metric=messages_in topic=orders window=5m")
                .build();
        }

        List<String> missing = new ArrayList<>();
        for (String requiredParam : definition.getRequiredParams()) {
            if (!StringUtils.hasText(resolveValue(query, requiredParam))) {
                missing.add(requiredParam);
            }
        }
        if (!missing.isEmpty()) {
            return RepairSuggestion.builder()
                .code("MISSING_REQUIRED_PARAMS")
                .message("Missing required Kafka metric parameters: " + String.join(", ", missing))
                .missingFields(missing)
                .exampleInput(exampleFor(query.getMetric()))
                .build();
        }

        if (!query.getWindow().matches("^\\d+[smhd]$")) {
            return RepairSuggestion.builder()
                .code("INVALID_WINDOW")
                .message("Invalid window '" + query.getWindow() + "'. Supported format examples: 5m, 15m, 1h.")
                .missingFields(List.of())
                .exampleInput(exampleFor(query.getMetric()))
                .build();
        }

        return null;
    }

    public String supportedMetrics() {
        return String.join(", ", properties.getMetrics().getDefinitions().keySet());
    }

    private String normalizeKey(String key) {
        return switch (key) {
            case "指标" -> "metric";
            case "主题" -> "topic";
            case "消费组" -> "group";
            case "时间窗" -> "window";
            default -> key;
        };
    }

    private String inferMetric(String input) {
        if (input.contains("lag")) {
            return "lag";
        }
        if (input.contains("messages_in")) {
            return "messages_in";
        }
        if (input.contains("bytes_in")) {
            return "bytes_in";
        }
        if (input.contains("bytes_out")) {
            return "bytes_out";
        }
        if (input.contains("under") || input.contains("副本")) {
            return "under_replicated_partitions";
        }
        return null;
    }

    private String resolveValue(KafkaMetricQuery query, String field) {
        return switch (field) {
            case "topic" -> query.getTopic();
            case "group" -> query.getGroup();
            case "broker" -> query.getBroker();
            case "cluster" -> query.getCluster();
            case "window" -> query.getWindow();
            default -> null;
        };
    }

    private String exampleFor(String metric) {
        return switch (metric) {
            case "lag" -> "metric=lag topic=orders group=order-service";
            case "messages_in" -> "metric=messages_in topic=orders window=5m";
            case "bytes_in" -> "metric=bytes_in topic=orders window=5m";
            case "bytes_out" -> "metric=bytes_out topic=orders window=5m";
            default -> "metric=under_replicated_partitions";
        };
    }
}
