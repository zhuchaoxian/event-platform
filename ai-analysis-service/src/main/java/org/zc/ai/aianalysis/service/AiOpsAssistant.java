package org.zc.ai.aianalysis.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AiOpsAssistant {

    @UserMessage("""
        Conversation intent: {{intent}}
        Preferred tool: {{preferredTool}}

        Conversation summary:
        {{conversationSummary}}

        Knowledge context:
        {{knowledgeContext}}

        Log context:
        {{logContext}}

        User request:
        {{userMessage}}
        """)
    String chat(
            @MemoryId String sessionId,                // 用于多轮会话区分用户
            @V("intent") String intent,                // 绑定 {{intent}}
            @V("preferredTool") String preferredTool,  // 绑定 {{preferredTool}}
            @V("conversationSummary") String conversationSummary, // 绑定 {{conversationSummary}}
            @V("knowledgeContext") String knowledgeContext,       // 绑定 {{knowledgeContext}}
            @V("logContext") String logContext,                   // 绑定 {{logContext}}
            @V("userMessage") String userMessage                  // 绑定 {{userMessage}}
    );
}
