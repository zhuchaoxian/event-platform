package org.zc.ai.aianalysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.dto.AiChatRequest;
import org.zc.ai.aianalysis.dto.AiChatResponse;
import org.zc.ai.aianalysis.dto.RepairSuggestion;
import org.zc.ai.aianalysis.memory.RedisChatMemoryStore;
import org.zc.ai.aianalysis.tool.KafkaMetricQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    private final AiAssistantGateway assistantGateway = mock(AiAssistantGateway.class);
    private final IntentClassifier intentClassifier = mock(IntentClassifier.class);
    private final KafkaMetricQueryParser kafkaMetricQueryParser = mock(KafkaMetricQueryParser.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final ElasticsearchLogSearchService logSearchService = mock(ElasticsearchLogSearchService.class);
    private final ToolExecutionRecorder toolExecutionRecorder = new ToolExecutionRecorder();
    private final RedisChatMemoryStore chatMemoryStore = mock(RedisChatMemoryStore.class);
    private final QuickReplyService quickReplyService = new QuickReplyService();
    private final AiChatResponseCache responseCache = mock(AiChatResponseCache.class);
    private final AiChatFallbackService fallbackService = mock(AiChatFallbackService.class);
    private final AiOpsProperties properties = new AiOpsProperties();

    private final AiChatService service = new AiChatService(
        assistantGateway,
        intentClassifier,
        kafkaMetricQueryParser,
        knowledgeBaseService,
        logSearchService,
        toolExecutionRecorder,
        chatMemoryStore,
        quickReplyService,
        responseCache,
        fallbackService,
        properties
    );

    @BeforeEach
    void setUp() {
        when(chatMemoryStore.getMessages(anyString())).thenReturn(List.of());
        when(chatMemoryStore.getConversationSummary(anyString())).thenReturn("");
        when(responseCache.keyFor(anyString(), any(), anyString(), anyString(), anyString(), anyInt())).thenReturn("cache-key");
        when(responseCache.get(anyString(), anyString(), any(), anyInt())).thenReturn(null);
    }

    @Test
    void shouldReturnCachedResponseWhenRedisHasEntry() {
        AiChatRequest request = new AiChatRequest();
        request.setSessionId("session-1");
        request.setMessage("help");

        when(intentClassifier.classify("help")).thenReturn(AiIntent.OPERATIONS_QA);
        AiChatResponse cached = AiChatResponse.builder()
            .sessionId("session-1")
            .intent(AiIntent.OPERATIONS_QA)
            .answer("cached")
            .toolCalls(List.of())
            .citations(List.of())
            .memoryWindow(1)
            .build();
        when(responseCache.get(anyString(), anyString(), any(), anyInt())).thenReturn(cached);

        var response = service.chat(request);

        assertThat(response.getAnswer()).isEqualTo("cached");
        verify(responseCache, times(1)).get(anyString(), anyString(), any(), anyInt());
        verifyNoInteractions(assistantGateway, knowledgeBaseService, logSearchService, fallbackService);
    }

    @Test
    void shouldReturnQuickReplyWithoutInvokingLlm() {
        AiChatRequest request = new AiChatRequest();
        request.setSessionId("session-2");
        request.setMessage("help");

        when(intentClassifier.classify("help")).thenReturn(AiIntent.OPERATIONS_QA);

        var response = service.chat(request);

        assertThat(response.getAnswer()).contains("Kafka metrics");
        assertThat(response.getToolCalls()).isEmpty();
        assertThat(response.getCitations()).isEmpty();
        verifyNoInteractions(assistantGateway, knowledgeBaseService, logSearchService, fallbackService);
        verify(kafkaMetricQueryParser, never()).parse(anyString());
    }

    @Test
    void shouldReturnRepairSuggestionWithoutInvokingLlm() {
        AiChatRequest request = new AiChatRequest();
        request.setSessionId("session-3");
        request.setMessage("metric=lag topic=orders");

        KafkaMetricQuery query = KafkaMetricQuery.builder()
            .metric("lag")
            .topic("orders")
            .rawInput(request.getMessage())
            .build();
        RepairSuggestion suggestion = RepairSuggestion.builder()
            .code("MISSING_GROUP")
            .message("Please provide group")
            .build();

        when(intentClassifier.classify(request.getMessage())).thenReturn(AiIntent.KAFKA_METRICS);
        when(kafkaMetricQueryParser.parse(request.getMessage())).thenReturn(query);
        when(kafkaMetricQueryParser.validate(query)).thenReturn(suggestion);

        var response = service.chat(request);

        assertThat(response.getAnswer()).isEqualTo("Please provide group");
        assertThat(response.getRepairSuggestion()).isEqualTo(suggestion);
        assertThat(response.getToolCalls()).isEmpty();
        assertThat(response.getCitations()).isEmpty();
        verifyNoInteractions(assistantGateway, knowledgeBaseService, logSearchService, fallbackService);
    }

    @Test
    void shouldFallbackToRuleReplyWhenLlmThrows() {
        AiChatRequest request = new AiChatRequest();
        request.setSessionId("session-4");
        request.setMessage("why is consumer lag high");

        when(intentClassifier.classify(request.getMessage())).thenReturn(AiIntent.OPERATIONS_QA);
        when(knowledgeBaseService.search(anyString())).thenReturn(List.of());
        when(knowledgeBaseService.formatForPrompt(anyList())).thenReturn("knowledge context");
        when(logSearchService.search(anyString(), anyString())).thenReturn(List.of());
        when(logSearchService.formatForPrompt(anyList())).thenReturn("log context");
        when(assistantGateway.chat(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenThrow(new RuntimeException("boom"));
        when(fallbackService.fallbackAnswer(any(), anyString(), anyList(), anyList()))
            .thenReturn("fallback answer");

        var response = service.chat(request);

        assertThat(response.getAnswer()).isEqualTo("fallback answer");
        verify(fallbackService).fallbackAnswer(
            AiIntent.OPERATIONS_QA,
            "why is consumer lag high",
            List.of(),
            List.of()
        );
    }
}
