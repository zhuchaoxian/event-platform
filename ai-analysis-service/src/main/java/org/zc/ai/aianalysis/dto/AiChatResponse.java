package org.zc.ai.aianalysis.dto;

import lombok.Builder;
import lombok.Data;
import org.zc.ai.aianalysis.service.AiIntent;

import java.util.List;

@Data
@Builder
/** 对话返回体。 */
public class AiChatResponse {
    private String sessionId;
    private AiIntent intent;
    private String answer;
    private List<ToolCallView> toolCalls;
    private List<CitationView> citations;
    private int memoryWindow;
    private RepairSuggestion repairSuggestion;
}
