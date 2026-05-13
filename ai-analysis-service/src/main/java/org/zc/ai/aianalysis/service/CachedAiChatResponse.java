package org.zc.ai.aianalysis.service;

import org.zc.ai.aianalysis.dto.CitationView;
import org.zc.ai.aianalysis.dto.RepairSuggestion;
import org.zc.ai.aianalysis.dto.ToolCallView;

import java.util.List;

public record CachedAiChatResponse(
    String answer,
    List<ToolCallView> toolCalls,
    List<CitationView> citations,
    RepairSuggestion repairSuggestion
) {
}
