package org.zc.ai.aianalysis.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RepairSuggestion {
    private String code;
    private String message;
    private List<String> missingFields;
    private String exampleInput;
}
