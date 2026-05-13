package org.zc.ai.aianalysis.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolCallView {
    private String toolName;
    private String arguments;
    private String resultPreview;
}
