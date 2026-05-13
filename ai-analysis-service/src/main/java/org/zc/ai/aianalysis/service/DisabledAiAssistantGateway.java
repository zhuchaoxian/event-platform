package org.zc.ai.aianalysis.service;

public class DisabledAiAssistantGateway implements AiAssistantGateway {

    private final String disabledMessage;

    public DisabledAiAssistantGateway(String disabledMessage) {
        this.disabledMessage = disabledMessage;
    }

    @Override
    public boolean enabled() {
        return false;
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
        return disabledMessage;
    }
}
