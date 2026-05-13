package org.zc.ai.aianalysis.service;

/**
 * 对底层大模型对话能力做一层网关抽象。
 * 这样在模型未配置、切换实现或增加兜底逻辑时，不需要改上层业务流程。
 */
public interface AiAssistantGateway {

    boolean enabled();

    String chat(
        String sessionId,
        String userMessage,
        String intent,
        String preferredTool,
        String conversationSummary,
        String knowledgeContext,
        String logContext
    );
}
