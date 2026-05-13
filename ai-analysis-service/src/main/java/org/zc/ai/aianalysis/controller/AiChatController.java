package org.zc.ai.aianalysis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zc.ai.aianalysis.dto.AiChatRequest;
import org.zc.ai.aianalysis.dto.AiChatResponse;
import org.zc.ai.aianalysis.dto.AiMessageView;
import org.zc.ai.aianalysis.dto.ReindexResponse;
import org.zc.ai.aianalysis.dto.SessionCreateResponse;
import org.zc.ai.aianalysis.service.AiChatService;
import org.zc.common.Result;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
/**
 * 智能运维服务对外 HTTP 入口。
 */
public class AiChatController {

    private final AiChatService aiChatService;

    /** 主对话接口，统一承载运维问答、Kafka 指标查询和错误输入修复建议。 */
    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return Result.ok(aiChatService.chat(request));
    }

    /** 创建一个新的会话 ID，供前端后续多轮对话复用。 */
    @PostMapping("/sessions")
    public Result<SessionCreateResponse> createSession() {
        return Result.ok(aiChatService.createSession());
    }

    /** 查看指定会话当前保留的消息窗口，主要用于调试和联调。 */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiMessageView>> messages(@PathVariable String sessionId) {
        return Result.ok(aiChatService.messages(sessionId));
    }

    /** 手动重建本地知识库索引。 */
    @PostMapping("/knowledge/reindex")
    public Result<ReindexResponse> reindex() {
        return Result.ok(aiChatService.reindexKnowledge());
    }
}
