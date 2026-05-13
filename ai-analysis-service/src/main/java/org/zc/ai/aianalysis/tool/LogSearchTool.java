package org.zc.ai.aianalysis.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zc.ai.aianalysis.service.ElasticsearchLogSearchService;
import org.zc.ai.aianalysis.service.LogSnippet;
import org.zc.ai.aianalysis.service.ToolExecutionRecorder;

import java.util.List;

@Component
@RequiredArgsConstructor
/** Agent 可调用的日志检索工具。 */
public class LogSearchTool {

    private final ElasticsearchLogSearchService logSearchService;
    private final ToolExecutionRecorder toolExecutionRecorder;
    private final ObjectMapper objectMapper;

    @Tool("Search Elasticsearch logs relevant to the user's troubleshooting request.")
    public String logSearchTool(String query, @ToolMemoryId Object memoryId) {
        List<LogSnippet> snippets = logSearchService.search(query, "logSearchTool");
        try {
            String result = objectMapper.writeValueAsString(snippets);
            toolExecutionRecorder.record(
                "logSearchTool",
                "query=%s,memoryId=%s".formatted(query, memoryId),
                result.length() > 400 ? result.substring(0, 400) : result
            );
            return result;
        } catch (Exception exception) {
            return "[]";
        }
    }
}
