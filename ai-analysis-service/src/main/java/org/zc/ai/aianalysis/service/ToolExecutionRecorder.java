package org.zc.ai.aianalysis.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ToolExecutionRecorder {

    private final ThreadLocal<List<ToolCallRecord>> records = ThreadLocal.withInitial(ArrayList::new);

    public void start() {
        records.set(new ArrayList<>());
    }

    public void record(String toolName, String arguments, String resultPreview) {
        records.get().add(new ToolCallRecord(toolName, arguments, resultPreview));
    }

    public List<ToolCallRecord> drain() {
        List<ToolCallRecord> current = new ArrayList<>(records.get());
        records.remove();
        return current;
    }
}
