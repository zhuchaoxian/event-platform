package org.zc.ai.aianalysis.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReindexResponse {
    private boolean success;
    private int indexedDocumentCount;
    private String message;
}
