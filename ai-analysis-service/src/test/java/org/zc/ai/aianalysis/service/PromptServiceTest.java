package org.zc.ai.aianalysis.service;

import org.junit.jupiter.api.Test;
import org.zc.ai.aianalysis.config.AiOpsProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptServiceTest {

    @Test
    void shouldBuildPromptWithCompactChineseInstructionsAndMetrics() {
        AiOpsProperties properties = new AiOpsProperties();
        properties.setSystemPrompt("Base prompt");
        AiOpsProperties.MetricDefinition lag = new AiOpsProperties.MetricDefinition();
        AiOpsProperties.MetricDefinition bytesIn = new AiOpsProperties.MetricDefinition();
        properties.getMetrics().setDefinitions(Map.of(
            "lag", lag,
            "bytes_in", bytesIn
        ));

        PromptService service = new PromptService(properties);

        String prompt = service.buildSystemPrompt();

        assertThat(prompt).contains("Default response language: Simplified Chinese.");
        assertThat(prompt).contains("Keep every answer compact: prefer one-sentence summary;");
        assertThat(prompt).contains("Do not use Markdown tables, long headings, or report-style sections.");
        assertThat(prompt).contains("For incident or troubleshooting analysis, state conclusion, likely cause, and first action only.");
        assertThat(prompt).contains("Supported Kafka metrics:");
        assertThat(prompt).contains("lag");
        assertThat(prompt).contains("bytes_in");
        assertThat(prompt).contains("When responding with Kafka metric results, answer in Simplified Chinese");
    }
}
