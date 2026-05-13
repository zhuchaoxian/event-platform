package org.zc.ai.aianalysis.config;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.memory.RedisChatMemoryStore;
import org.zc.ai.aianalysis.service.AiAssistantGateway;
import org.zc.ai.aianalysis.service.AiOpsAssistant;
import org.zc.ai.aianalysis.service.DisabledAiAssistantGateway;
import org.zc.ai.aianalysis.service.LangChainAiAssistantGateway;
import org.zc.ai.aianalysis.service.PromptService;
import org.zc.ai.aianalysis.tool.KafkaMetricsTool;
import org.zc.ai.aianalysis.tool.LogSearchTool;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class AiOpsConfiguration {

    @Bean
    AiAssistantGateway aiAssistantGateway(
        AiOpsProperties properties,
        RedisChatMemoryStore chatMemoryStore,
        PromptService promptService,
        KafkaMetricsTool kafkaMetricsTool,
        LogSearchTool logSearchTool
    ) {
        if (!properties.isEnabled() || !hasModelConfiguration(properties.getModel())) {
            return new DisabledAiAssistantGateway(properties.getDisabledMessage());
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .baseUrl(properties.getModel().getBaseUrl())
            .apiKey(properties.getModel().getApiKey())
            .modelName(properties.getModel().getModelName())
            .temperature(properties.getModel().getTemperature())
            .timeout(timeout(properties.getModel().getTimeout()))
            .build();

        List<Object> tools = new ArrayList<>();
        if (properties.getTools().isKafkaMetricsEnabled()) {
            tools.add(kafkaMetricsTool);
        }
        if (properties.getTools().isLogSearchEnabled()) {
            tools.add(logSearchTool);
        }

        AiOpsAssistant assistant = AiServices.builder(AiOpsAssistant.class)
            .chatModel(chatModel)
            .systemMessage(promptService.buildSystemPrompt())
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(effectiveMaxMessages(properties))
                .chatMemoryStore(chatMemoryStore)
                .build())
            .tools(tools)
            .build();

        return new LangChainAiAssistantGateway(assistant);
    }

    private boolean hasModelConfiguration(AiOpsProperties.Model model) {
        return StringUtils.hasText(model.getBaseUrl())
            && StringUtils.hasText(model.getApiKey())
            && StringUtils.hasText(model.getModelName());
    }

    private Duration timeout(Duration duration) {
        return duration == null ? Duration.ofSeconds(30) : duration;
    }

    private int effectiveMaxMessages(AiOpsProperties properties) {
        if (!properties.getMemory().isSummaryEnabled()) {
            return properties.getMemory().getMaxMessages();
        }
        int summaryWindow = properties.getMemory().getSummaryBatchSize() + properties.getMemory().getSummaryRetainMessages();
        return Math.max(properties.getMemory().getMaxMessages(), summaryWindow);
    }
}
