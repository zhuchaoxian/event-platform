package org.zc.ai.aianalysis.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.service.ConversationSummaryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final AiOpsProperties properties;
    private final ConversationSummaryService conversationSummaryService;
    private final Map<String, String> localFallback = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = readValue(memoryKey(memoryId));
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        List<ChatMessage> messagesToStore = messages == null ? List.of() : new ArrayList<>(messages);
        if (shouldSummarize(messagesToStore)) {
            String existingSummary = getConversationSummary(String.valueOf(memoryId));
            SummaryUpdate summaryUpdate = summarize(existingSummary, messagesToStore);
            if (summaryUpdate.updated()) {
                writeValue(summaryKey(memoryId), summaryUpdate.summary());
                messagesToStore = summaryUpdate.messages();
            }
        }
        writeValue(memoryKey(memoryId), ChatMessageSerializer.messagesToJson(messagesToStore));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        deleteValue(memoryKey(memoryId));
        deleteValue(summaryKey(memoryId));
    }

    public String getConversationSummary(String sessionId) {
        String summary = readValue(summaryKey(sessionId));
        return StringUtils.hasText(summary) ? summary : "";
    }

    private SummaryUpdate summarize(String existingSummary, List<ChatMessage> messages) {
        List<ChatMessage> working = new ArrayList<>(messages);
        String summary = existingSummary;
        boolean updated = false;

        while (working.size() >= summaryTriggerSize()) {
            List<ChatMessage> batch = new ArrayList<>(working.subList(0, summaryBatchSize()));
            var summarized = conversationSummaryService.summarize(summary, batch);
            if (summarized.isEmpty()) {
                return new SummaryUpdate(existingSummary, messages, false);
            }
            summary = summarized.get();
            working = new ArrayList<>(working.subList(summaryBatchSize(), working.size()));
            updated = true;
        }
        return new SummaryUpdate(summary, working, updated);
    }

    private boolean shouldSummarize(List<ChatMessage> messages) {
        return properties.getMemory().isSummaryEnabled() && messages.size() >= summaryTriggerSize();
    }

    private int summaryTriggerSize() {
        return summaryBatchSize() + summaryRetainMessages();
    }

    private int summaryBatchSize() {
        return Math.max(1, properties.getMemory().getSummaryBatchSize());
    }

    private int summaryRetainMessages() {
        return Math.max(1, properties.getMemory().getSummaryRetainMessages());
    }

    private String memoryKey(Object memoryId) {
        return properties.getMemory().getKeyPrefix() + memoryId;
    }

    private String summaryKey(Object memoryId) {
        return properties.getMemory().getSummaryKeyPrefix() + memoryId;
    }

    private String readValue(String key) {
        String value = localFallback.get(key);
        try {
            String redisValue = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(redisValue)) {
                value = redisValue;
                localFallback.put(key, redisValue);
            }
        } catch (RuntimeException ignored) {
            // Keep local fallback to avoid failing the whole request path when Redis is not available.
        }
        return value;
    }

    private void writeValue(String key, String value) {
        localFallback.put(key, value);
        try {
            redisTemplate.opsForValue().set(key, value, properties.getMemory().getTtl());
        } catch (RuntimeException ignored) {
            // Local fallback already updated.
        }
    }

    private void deleteValue(String key) {
        localFallback.remove(key);
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ignored) {
            // Ignore Redis errors during cleanup.
        }
    }

    private record SummaryUpdate(String summary, List<ChatMessage> messages, boolean updated) {
    }
}
