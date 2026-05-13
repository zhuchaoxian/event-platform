package org.zc.ai.aianalysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai.ops")
public class AiOpsProperties {

    private boolean enabled = true;
    private String disabledMessage = "AI assistant is not configured yet.";
    private String systemPrompt = "";
    private Memory memory = new Memory();
    private Model model = new Model();
    private Embedding embedding = new Embedding();
    private Rag rag = new Rag();
    private Elasticsearch elasticsearch = new Elasticsearch();
    private Prometheus prometheus = new Prometheus();
    private Cache cache = new Cache();
    private Tools tools = new Tools();
    private Metrics metrics = new Metrics();

    @Data
    public static class Memory {
        private int maxMessages = 20;
        private String keyPrefix = "ai:chat:memory:";
        private Duration ttl = Duration.ofDays(7);
        private boolean summaryEnabled = true;
        private String summaryKeyPrefix = "ai:chat:summary:";
        private int summaryBatchSize = 20;
        private int summaryRetainMessages = 8;
    }

    @Data
    public static class Model {
        private String provider = "openai-compatible";
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Double temperature = 0.2d;
        private Duration timeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Embedding {
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Duration timeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Rag {
        private boolean enabled = true;
        private String documentsPath = "docs/ai-ops";
        private boolean recursive = true;
        private boolean autoReindexOnStartup = false;
        private String indexName = "ai_ops_knowledge";
        private int topK = 4;
        private double minScore = 0.35d;
        private int maxSegmentSize = 800;// 每个文本块最大长度 chunk size
        private int maxOverlapSize = 120;// 相邻 chunk 重叠部分 没有这个会语义不通顺 120 是一个经验值
    }

    @Data
    public static class Elasticsearch {
        private boolean enabled;
        private String url = "http://localhost:9200";
        private String username;
        private String password;
        private String apiKey;
        private String knowledgeIndexName = "ai_ops_knowledge";
        private String logsIndexPattern = "event-platform-logs-*";
        private int logSize = 3;
    }

    @Data
    public static class Prometheus {
        private boolean enabled;
        private String baseUrl = "http://localhost:9090";
        private String defaultWindow = "5m";
        private Duration timeout = Duration.ofSeconds(10);
    }

    @Data
    public static class Cache {
        private String responseKeyPrefix = "ai:chat:response:";
        private Duration responseTtl = Duration.ofMinutes(2);
        private boolean fallbackEnabled = true;
    }

    @Data
    public static class Tools {
        private boolean kafkaMetricsEnabled = true;
        private boolean logSearchEnabled = true;
    }

    @Data
    public static class Metrics {
        private Map<String, MetricDefinition> definitions = new LinkedHashMap<>();
    }

    @Data
    public static class MetricDefinition {
        private String description;
        private String queryTemplate;
        private List<String> requiredParams = new ArrayList<>();
    }
}
