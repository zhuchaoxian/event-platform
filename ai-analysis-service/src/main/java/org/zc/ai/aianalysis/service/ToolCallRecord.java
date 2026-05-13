package org.zc.ai.aianalysis.service;

public record ToolCallRecord(
    String toolName,
    String arguments,
    String resultPreview
) {
}
