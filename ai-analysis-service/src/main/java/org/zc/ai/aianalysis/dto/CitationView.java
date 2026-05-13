package org.zc.ai.aianalysis.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CitationView {
    private String sourceType;
    private String sourceId;
    private String title;
    private String snippet;
}
