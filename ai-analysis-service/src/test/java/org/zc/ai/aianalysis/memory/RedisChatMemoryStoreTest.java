package org.zc.ai.aianalysis.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.service.ConversationSummaryService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisChatMemoryStoreTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ConversationSummaryService summaryService = mock(ConversationSummaryService.class);
    private final AiOpsProperties properties = new AiOpsProperties();
    private final Map<String, String> redisData = new ConcurrentHashMap<>();
    private final RedisChatMemoryStore store = new RedisChatMemoryStore(redisTemplate, properties, summaryService);

    @BeforeEach
    void setUp() {
        properties.getMemory().setTtl(Duration.ofDays(1));
        properties.getMemory().setSummaryEnabled(true);
        properties.getMemory().setSummaryBatchSize(20);
        properties.getMemory().setSummaryRetainMessages(8);
        properties.getMemory().setKeyPrefix("ai:chat:memory:");
        properties.getMemory().setSummaryKeyPrefix("ai:chat:summary:");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisData.get(invocation.getArgument(0, String.class)));
        doAnswer(invocation -> {
            redisData.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        redisData.clear();
    }

    @Test
    void shouldSummarizeFirstTwentyMessagesAndKeepRecentEight() {
        List<ChatMessage> messages = IntStream.range(0, 28)
            .mapToObj(index -> (ChatMessage) UserMessage.from("message-" + index))
            .toList();
        when(summaryService.summarize(anyString(), anyList())).thenReturn(Optional.of("summary"));

        store.updateMessages("session-1", messages);

        List<?> storedMessages = store.getMessages("session-1");
        assertThat(storedMessages).hasSize(8);
        assertThat(store.getConversationSummary("session-1")).isEqualTo("summary");
        verify(summaryService).summarize("", messages.subList(0, 20));
    }
}
