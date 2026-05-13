package org.zc.ai.aianalysis.service;

import org.junit.jupiter.api.Test;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.tool.KafkaMetricQuery;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaMetricQueryParserTest {

    private final KafkaMetricQueryParser parser = new KafkaMetricQueryParser(properties());

    @Test
    void shouldParseExplicitKeyValueRequest() {
        KafkaMetricQuery query = parser.parse("metric=lag topic=orders group=order-service window=5m");

        assertThat(query.getMetric()).isEqualTo("lag");
        assertThat(query.getTopic()).isEqualTo("orders");
        assertThat(query.getGroup()).isEqualTo("order-service");
        assertThat(query.getWindow()).isEqualTo("5m");
        assertThat(parser.validate(query)).isNull();
    }

    @Test
    void shouldReportMissingFields() {
        KafkaMetricQuery query = parser.parse("kafka metric=lag topic=orders");

        assertThat(parser.validate(query)).isNotNull();
        assertThat(parser.validate(query).getMissingFields()).containsExactly("group");
    }

    private AiOpsProperties properties() {
        AiOpsProperties properties = new AiOpsProperties();
        AiOpsProperties.MetricDefinition lag = new AiOpsProperties.MetricDefinition();
        lag.setQueryTemplate("sum(kafka_consumergroup_lag{topic=\"%s\",consumergroup=\"%s\"})");
        lag.setRequiredParams(List.of("topic", "group"));
        properties.getMetrics().setDefinitions(Map.of("lag", lag));
        return properties;
    }
}
