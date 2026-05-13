package org.zc.ai.aianalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LangChainAiAssistantGateway implements AiAssistantGateway {

    private final AiOpsAssistant assistant;

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String chat(
        String sessionId,
        String userMessage,
        String intent,
        String preferredTool,
        String conversationSummary,
        String knowledgeContext,
        String logContext
    ) {
        log.info("sessionId: {}, userMessage: {}, intent: {}, preferredTool: {}, conversationSummary: {}, knowledgeContext: {}, logContext: {}", sessionId, userMessage, intent, preferredTool, conversationSummary, knowledgeContext, logContext);
        return assistant.chat(sessionId, intent, preferredTool, conversationSummary, knowledgeContext, logContext, userMessage);
    }
}
