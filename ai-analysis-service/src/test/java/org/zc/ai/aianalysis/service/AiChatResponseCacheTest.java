package org.zc.ai.aianalysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.dto.AiChatResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatResponseCacheTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiOpsProperties properties = new AiOpsProperties();
    private final Map<String, String> redisData = new ConcurrentHashMap<>();
    private final AiChatResponseCache cache = new AiChatResponseCache(redisTemplate, objectMapper, properties);

    @BeforeEach
    void setUp() {
        properties.getCache().setResponseKeyPrefix("ai:chat:response:");
        properties.getCache().setResponseTtl(Duration.ofMinutes(2));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisData.get(invocation.getArgument(0, String.class)));
        doAnswer(invocation -> {
            redisData.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        redisData.clear();
    }

    @Test
    void shouldRoundTripCachedResponse() {
        String key = cache.keyFor("session", AiIntent.OPERATIONS_QA, "auto", "hello", "", 0);
        AiChatResponse response = AiChatResponse.builder()
            .sessionId("session")
            .intent(AiIntent.OPERATIONS_QA)
            .answer("cached answer")
            .toolCalls(List.of())
            .citations(List.of())
            .memoryWindow(0)
            .build();

        cache.put(key, response);

        AiChatResponse loaded = cache.get(key, "session", AiIntent.OPERATIONS_QA, 3);
        assertThat(loaded).isNotNull();
        assertThat(loaded.getAnswer()).isEqualTo("cached answer");
        assertThat(loaded.getMemoryWindow()).isEqualTo(3);
    }
}
