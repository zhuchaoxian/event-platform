package org.zc.ai.aianalysis.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.service.ToolExecutionRecorder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
/**
 * Agent 可调用的 Kafka 指标工具。
 * 底层通过 Prometheus HTTP API 查询指标结果。
 */
public class KafkaMetricsTool {

    private final AiOpsProperties properties;
    private final ObjectMapper objectMapper;
    private final ToolExecutionRecorder toolExecutionRecorder;

    @Tool("Query Kafka metrics from Prometheus. Required fields depend on metric name. Supported metrics include lag, messages_in, bytes_in, bytes_out, under_replicated_partitions.")
    public String kafkaMetricsTool(
        String metric,
        String topic,
        String group,
        String broker,
        String cluster,
        String window,
        @ToolMemoryId Object memoryId
    ) {
        Map<String, Object> payload = query(metric, topic, group, broker, cluster, window);
        try {
            String result = objectMapper.writeValueAsString(payload);
            toolExecutionRecorder.record(
                "kafkaMetricsTool",
                "metric=%s,topic=%s,group=%s,broker=%s,cluster=%s,window=%s,memoryId=%s".formatted(metric, topic, group, broker, cluster, window, memoryId),
                truncate(result)
            );
            return result;
        } catch (Exception exception) {
            return "{\"status\":\"error\",\"message\":\"" + exception.getMessage() + "\"}";
        }
    }

    /** 对外暴露一个普通 Java 方法，便于非 Agent 场景复用指标查询逻辑。 */
    public Map<String, Object> query(String metric, String topic, String group, String broker, String cluster, String window) {
        if (!properties.getPrometheus().isEnabled()) {
            return Map.of(
                "status", "disabled",
                "message", "Prometheus integration is disabled."
            );
        }
        AiOpsProperties.MetricDefinition definition = properties.getMetrics().getDefinitions().get(metric);
        if (definition == null) {
            return Map.of(
                "status", "invalid_metric",
                "message", "Unsupported metric: " + metric,
                "supportedMetrics", properties.getMetrics().getDefinitions().keySet()
            );
        }
        String effectiveWindow = StringUtils.hasText(window) ? window : properties.getPrometheus().getDefaultWindow();
        List<String> params = definition.getRequiredParams().stream()
            .map(param -> switch (param) {
                case "topic" -> topic;
                case "group" -> group;
                case "broker" -> broker;
                case "cluster" -> cluster;
                case "window" -> effectiveWindow;
                default -> "";
            })
            .toList();
        // PromQL 模板走配置驱动，后续新增指标映射时不需要改 Java 代码。
        String query = definition.getRequiredParams().contains("window")
            ? definition.getQueryTemplate().formatted(params.toArray())
            : fillTemplate(definition.getQueryTemplate(), topic, group, broker, cluster, effectiveWindow);

        try {
            String response = RestClient.create(properties.getPrometheus().getBaseUrl())
                .get()
                .uri(UriComponentsBuilder.fromPath("/api/v1/query")
                    .queryParam("query", query)
                    .build(true)
                    .toUriString())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
            return Map.of(
                "status", "ok",
                "metric", metric,
                "window", effectiveWindow,
                "query", query,
                "response", objectMapper.readTree(response)
            );
        } catch (Exception exception) {
            return Map.of(
                "status", "error",
                "metric", metric,
                "window", effectiveWindow,
                "query", query,
                "message", exception.getMessage()
            );
        }
    }

    private String fillTemplate(String template, String topic, String group, String broker, String cluster, String window) {
        int placeholders = template.split("%s", -1).length - 1;
        return switch (placeholders) {
            case 0 -> template;
            case 1 -> template.formatted(firstNonBlank(topic, broker, cluster, window));
            case 2 -> template.formatted(firstNonBlank(topic, broker, cluster), firstNonBlank(group, window));
            case 3 -> template.formatted(firstNonBlank(topic, ""), firstNonBlank(group, ""), firstNonBlank(window, ""));
            default -> template.formatted(firstNonBlank(topic, ""), firstNonBlank(group, ""), firstNonBlank(broker, ""), firstNonBlank(cluster, ""), firstNonBlank(window, ""));
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String truncate(String result) {
        return result.length() > 400 ? result.substring(0, 400) : result;
    }
}
