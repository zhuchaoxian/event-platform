package org.zc.ai.aianalysis.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.config.AiOpsProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationSummaryService {

    private final AiOpsProperties properties;
    private volatile OpenAiChatModel summaryModel;

    public ConversationSummaryService(AiOpsProperties properties) {
        this.properties = properties;
    }

    public Optional<String> summarize(String existingSummary, List<ChatMessage> messages) {
        if (!properties.getMemory().isSummaryEnabled()
            || !hasModelConfiguration()
            || messages == null
            || messages.isEmpty()) {
            return Optional.empty();
        }

        String prompt = buildPrompt(existingSummary, messages);
        try {
            String summary = model().chat(prompt);
            if (!StringUtils.hasText(summary)) {
                return Optional.empty();
            }
            return Optional.of(summary.trim());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private OpenAiChatModel model() {
        OpenAiChatModel current = summaryModel;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (summaryModel == null) {
                summaryModel = OpenAiChatModel.builder()
                    .baseUrl(properties.getModel().getBaseUrl())
                    .apiKey(properties.getModel().getApiKey())
                    .modelName(properties.getModel().getModelName())
                    .temperature(properties.getModel().getTemperature())
                    .timeout(timeout(properties.getModel().getTimeout()))
                    .build();
            }
            return summaryModel;
        }
    }

    private boolean hasModelConfiguration() {
        return StringUtils.hasText(properties.getModel().getBaseUrl())
            && StringUtils.hasText(properties.getModel().getApiKey())
            && StringUtils.hasText(properties.getModel().getModelName());
    }

    private String buildPrompt(String existingSummary, List<ChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("Summarize the conversation history for future troubleshooting context.")
            .append(System.lineSeparator())
            .append("Keep only facts, decisions, tool findings, and unresolved questions.")
            .append(System.lineSeparator())
            .append("Do not invent information.")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Existing summary:")
            .append(System.lineSeparator())
            .append(StringUtils.hasText(existingSummary) ? existingSummary : "None.")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("New messages:")
            .append(System.lineSeparator());

        for (ChatMessage message : messages) {
            builder.append("- ").append(render(message)).append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private String render(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return "USER: " + userMessage.singleText();
        }
        if (message instanceof AiMessage aiMessage) {
            return "AI: " + aiMessage.text();
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            return "TOOL: " + toolMessage.text();
        }
        return message.type() + ": " + String.valueOf(message);
    }

    private Duration timeout(Duration configuredTimeout) {
        return configuredTimeout == null ? Duration.ofSeconds(30) : configuredTimeout;
    }
}
