package org.zc.ai.aianalysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
/** 对话请求体。 */
public class AiChatRequest {

    /** 会话 ID；为空时服务端会自动生成。 */
    private String sessionId;

    /** 用户本轮输入内容。 */
    @NotBlank(message = "message must not be blank")
    private String message;

    /** 可选的工具偏好，用于强制偏向某个工具。 */
    private String preferredTool;

    /** 预留上下文字段，方便后续接入前端额外上下文。 */
    private Map<String, String> context = new LinkedHashMap<>();
}
