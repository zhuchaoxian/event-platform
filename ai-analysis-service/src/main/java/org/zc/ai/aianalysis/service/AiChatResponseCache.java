package org.zc.ai.aianalysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.dto.AiChatResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AiChatResponseCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiOpsProperties properties;

    public String keyFor(
        String sessionId,
        AiIntent intent,
        String preferredTool,
        String message,
        String conversationSummary,
        int memoryWindow
    ) {
        String fingerprint = String.join(
            "|",
            normalize(sessionId),
            intent == null ? AiIntent.UNKNOWN.name() : intent.name(),
            normalize(preferredTool),
            normalize(message),
            normalize(conversationSummary),
            String.valueOf(memoryWindow)
        );
        return properties.getCache().getResponseKeyPrefix()
            + DigestUtils.md5DigestAsHex(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    public AiChatResponse get(String key, String sessionId, AiIntent intent, int memoryWindow) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            CachedAiChatResponse cached = objectMapper.readValue(json, CachedAiChatResponse.class);
            return AiChatResponse.builder()
                .sessionId(sessionId)
                .intent(intent)
                .answer(cached.answer())
                .toolCalls(cached.toolCalls())
                .citations(cached.citations())
                .memoryWindow(memoryWindow)
                .repairSuggestion(cached.repairSuggestion())
                .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    public void put(String key, AiChatResponse response) {
        try {
            CachedAiChatResponse cached = new CachedAiChatResponse(
                response.getAnswer(),
                response.getToolCalls() == null ? List.of() : List.copyOf(response.getToolCalls()),
                response.getCitations() == null ? List.of() : List.copyOf(response.getCitations()),
                response.getRepairSuggestion()
            );
            redisTemplate.opsForValue().set(
                key,
                objectMapper.writeValueAsString(cached),
                properties.getCache().getResponseTtl()
            );
        } catch (Exception ignored) {
            // Cache failures must not break the request path.
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
