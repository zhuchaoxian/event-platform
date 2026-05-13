package org.zc.ai.aianalysis.service;

// 自动定义属性 为private final 自动生成set get toString hashcode
public record LogSnippet(
    String documentId,
    String index,
    String summary
) {
}
