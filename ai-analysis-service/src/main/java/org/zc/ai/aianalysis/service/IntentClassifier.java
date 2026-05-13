package org.zc.ai.aianalysis.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * 轻量级意图分类器。
 * V1 不引入单独分类模型，先用规则把 Kafka 指标查询和普通运维问答分开。
 */
public class IntentClassifier {

    private static final List<String> KAFKA_HINTS = List.of(
        "kafka",
        "lag",
        "topic",
        "consumer group",
        "consumergroup",
        "broker",
        "messages_in",
        "bytes_in",
        "bytes_out",
        "under_replicated_partitions",
        "消费组",
        "主题",
        "指标"
    );

    public AiIntent classify(String message) {
        String lowerCase = message == null ? "" : message.toLowerCase();
        if (KAFKA_HINTS.stream().anyMatch(lowerCase::contains)) {
            return AiIntent.KAFKA_METRICS;
        }
        return lowerCase.isBlank() ? AiIntent.UNKNOWN : AiIntent.OPERATIONS_QA;
    }
}
