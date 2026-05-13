package org.zc.ai.aianalysis.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.dto.AiChatRequest;
import org.zc.ai.aianalysis.dto.AiChatResponse;
import org.zc.ai.aianalysis.dto.AiMessageView;
import org.zc.ai.aianalysis.dto.CitationView;
import org.zc.ai.aianalysis.dto.ReindexResponse;
import org.zc.ai.aianalysis.dto.RepairSuggestion;
import org.zc.ai.aianalysis.dto.SessionCreateResponse;
import org.zc.ai.aianalysis.dto.ToolCallView;
import org.zc.ai.aianalysis.memory.RedisChatMemoryStore;
import org.zc.ai.aianalysis.tool.KafkaMetricQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final AiAssistantGateway assistantGateway;
    private final IntentClassifier intentClassifier;
    private final KafkaMetricQueryParser kafkaMetricQueryParser;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ElasticsearchLogSearchService logSearchService;
    private final ToolExecutionRecorder toolExecutionRecorder;
    private final RedisChatMemoryStore chatMemoryStore;
    private final QuickReplyService quickReplyService;
    private final AiChatResponseCache responseCache;
    private final AiChatFallbackService fallbackService;
    private final AiOpsProperties properties;

    public AiChatResponse chat(AiChatRequest request) {
        String sessionId = StringUtils.hasText(request.getSessionId()) ? request.getSessionId() : UUID.randomUUID().toString();
        AiIntent intent = intentClassifier.classify(request.getMessage());
        int memoryWindow = currentMemoryWindow(sessionId);
        String preferredTool = effectivePreferredTool(request.getPreferredTool(), intent);
        String conversationSummary = chatMemoryStore.getConversationSummary(sessionId);
        String cacheKey = responseCache.keyFor(
            sessionId,
            intent,
            preferredTool,
            request.getMessage(),
            conversationSummary,
            memoryWindow
        );

        AiChatResponse cached = responseCache.get(cacheKey, sessionId, intent, memoryWindow);
        log.info("redis cached = {}", cached);
        if (cached != null) {
            return cached;
        }

        RepairSuggestion repairSuggestion = null;
        if (intent == AiIntent.KAFKA_METRICS) {
            KafkaMetricQuery query = kafkaMetricQueryParser.parse(request.getMessage());
            repairSuggestion = kafkaMetricQueryParser.validate(query);
            if (repairSuggestion != null) {
                AiChatResponse response = AiChatResponse.builder()
                    .sessionId(sessionId)
                    .intent(intent)
                    .answer(repairSuggestion.getMessage())
                    .toolCalls(List.of())
                    .citations(List.of())
                    .memoryWindow(memoryWindow)
                    .repairSuggestion(repairSuggestion)
                    .build();
                responseCache.put(cacheKey, response);
                return response;
            }
        }

        if (quickReplyService.supports(request.getMessage())) {
            AiChatResponse response = AiChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .answer(quickReplyService.reply(request.getMessage()))
                .toolCalls(List.of())
                .citations(List.of())
                .memoryWindow(memoryWindow)
                .repairSuggestion(null)
                .build();
            responseCache.put(cacheKey, response);
            return response;
        }

        List<KnowledgeSnippet> knowledgeSnippets = knowledgeBaseService.search(request.getMessage());
        List<LogSnippet> logSnippets = logSearchService.search(request.getMessage(), request.getPreferredTool());
        List<CitationView> citations = citations(knowledgeSnippets, logSnippets);

        toolExecutionRecorder.start();
        try {
            String answer = assistantGateway.chat(
                sessionId,
                request.getMessage(),
                intent.name(),
                preferredTool,
                promptSummary(conversationSummary),
                knowledgeBaseService.formatForPrompt(knowledgeSnippets),
                logSearchService.formatForPrompt(logSnippets)
            );
            if (!StringUtils.hasText(answer)) {
                throw new IllegalStateException("LLM returned an empty answer");
            }
            AiChatResponse response = AiChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .answer(answer)
                .toolCalls(drainToolCalls())
                .citations(citations)
                .memoryWindow(currentMemoryWindow(sessionId))
                .repairSuggestion(repairSuggestion)
                .build();
            responseCache.put(cacheKey, response);
            return response;
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            log.error("error = {}",exception.getMessage());
            AiChatResponse response = AiChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .answer(fallbackAnswer(intent, request.getMessage(), knowledgeSnippets, logSnippets))
                .toolCalls(drainToolCalls())
                .citations(citations)
                .memoryWindow(currentMemoryWindow(sessionId))
                .repairSuggestion(repairSuggestion)
                .build();
            responseCache.put(cacheKey, response);
            return response;
        }
    }

    public SessionCreateResponse createSession() {
        return new SessionCreateResponse(UUID.randomUUID().toString());
    }

    public List<AiMessageView> messages(String sessionId) {
        return chatMemoryStore.getMessages(sessionId).stream()
            .map(this::toView)
            .toList();
    }

    public ReindexResponse reindexKnowledge() {
        return knowledgeBaseService.reindex();
    }

    private List<ToolCallView> drainToolCalls() {
        return toolExecutionRecorder.drain().stream()
            .map(record -> ToolCallView.builder()
                .toolName(record.toolName())
                .arguments(record.arguments())
                .resultPreview(record.resultPreview())
                .build())
            .toList();
    }

    private List<CitationView> citations(List<KnowledgeSnippet> knowledgeSnippets, List<LogSnippet> logSnippets) {
        List<CitationView> citations = new ArrayList<>();
        knowledgeSnippets.forEach(snippet -> citations.add(CitationView.builder()
            .sourceType("knowledge")
            .sourceId(snippet.sourceId())
            .title(snippet.title())
            .snippet(snippet.text())
            .build()));
        logSnippets.forEach(snippet -> citations.add(CitationView.builder()
            .sourceType("log")
            .sourceId(snippet.documentId())
            .title(snippet.index())
            .snippet(snippet.summary())
            .build()));
        return citations;
    }

    private AiMessageView toView(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return new AiMessageView(message.type().name(), userMessage.singleText());
        }
        if (message instanceof AiMessage aiMessage) {
            return new AiMessageView(message.type().name(), aiMessage.text());
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            return new AiMessageView(message.type().name(), toolMessage.text());
        }
        return new AiMessageView(message.type().name(), String.valueOf(message));
    }

    private int currentMemoryWindow(String sessionId) {
        return chatMemoryStore.getMessages(sessionId).size();
    }

    private String effectivePreferredTool(String preferredTool, AiIntent intent) {
        return StringUtils.hasText(preferredTool) ? preferredTool : defaultTool(intent);
    }

    private String defaultTool(AiIntent intent) {
        return intent == AiIntent.KAFKA_METRICS ? "kafkaMetricsTool" : "auto";
    }

    private String promptSummary(String conversationSummary) {
        return StringUtils.hasText(conversationSummary) ? conversationSummary : "No prior conversation summary.";
    }

    private String fallbackAnswer(
        AiIntent intent,
        String userMessage,
        List<KnowledgeSnippet> knowledgeSnippets,
        List<LogSnippet> logSnippets
    ) {
        if (!properties.getCache().isFallbackEnabled()) {
            return "LLM service is temporarily unavailable. Please retry later.";
        }
        return fallbackService.fallbackAnswer(intent, userMessage, knowledgeSnippets, logSnippets);
    }
}
