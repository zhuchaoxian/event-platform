package org.zc.ai.aianalysis.tool;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KafkaMetricQuery {
    private String metric;
    private String topic;
    private String group;
    private String broker;
    private String cluster;
    private String window;
    private String rawInput;
}
