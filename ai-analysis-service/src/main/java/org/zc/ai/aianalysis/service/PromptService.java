package org.zc.ai.aianalysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zc.ai.aianalysis.config.AiOpsProperties;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final AiOpsProperties properties;

    public String buildSystemPrompt() {
        StringBuilder builder = new StringBuilder(properties.getSystemPrompt().trim());
        builder.append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Default response language: Simplified Chinese.")
            .append(System.lineSeparator())
            .append("Keep every answer compact: prefer one-sentence summary; add at most one short follow-up sentence only when necessary.")
            .append(System.lineSeparator())
            .append("Do not use Markdown tables, long headings, or report-style sections.")
            .append(System.lineSeparator())
            .append("For incident or troubleshooting analysis, state conclusion, likely cause, and first action only.")
            .append(System.lineSeparator())
            .append("Supported Kafka metrics: ")
            .append(String.join(", ", properties.getMetrics().getDefinitions().keySet()))
            .append(System.lineSeparator())
            .append("When responding with Kafka metric results, answer in Simplified Chinese and include metric name, labels used, time window, and a short operational interpretation in plain text.");
        return builder.toString();
    }
}
