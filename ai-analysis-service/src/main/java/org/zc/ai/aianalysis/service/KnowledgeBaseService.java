package org.zc.ai.aianalysis.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zc.ai.aianalysis.config.AiOpsProperties;
import org.zc.ai.aianalysis.dto.ReindexResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
/**
 * 本地文档知识库服务。
 * 负责文档切分、向量化、重建索引和查询召回。
 */
public class KnowledgeBaseService {

    private final AiOpsProperties properties;
    private final AtomicBoolean indexed = new AtomicBoolean(false);

    private volatile EmbeddingModel embeddingModel;
    private volatile EmbeddingStore<TextSegment> embeddingStore;

    /** 按查询文本召回知识片段。 */
    public List<KnowledgeSnippet> search(String query) {
        if (!properties.getRag().isEnabled()) {
            return List.of();
        }
        if (!indexed.get()) {
            // 首次查询时再懒加载索引，避免文档目录或向量模型尚未准备好时影响服务启动。
            reindex();
        }
        if (!isSearchReady()) {
            return List.of();
        }
        try {
            Embedding embedding = embeddingModel.embed(query).content();
            var result = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(properties.getRag().getTopK())
                .minScore(properties.getRag().getMinScore())
                .build());
            return result.matches().stream()
                .sorted(Comparator.comparingDouble(EmbeddingMatch<TextSegment>::score).reversed())
                .map(match -> new KnowledgeSnippet(
                    match.embedded().metadata().getString("source"),
                    match.embedded().metadata().getString("title"),
                    match.embedded().text()
                ))
                .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /** 重建知识库索引。 */
    public synchronized ReindexResponse reindex() {
        if (!properties.getRag().isEnabled()) {
            return ReindexResponse.builder()
                .success(false)
                .indexedDocumentCount(0)
                .message("RAG is disabled.")
                .build();
        }
        try {
            initializeInfrastructure();
            if (!isSearchReady()) {
                return ReindexResponse.builder()
                    .success(false)
                    .indexedDocumentCount(0)
                    .message("Embedding model is not configured.")
                    .build();
            }
            Path documentsPath = Path.of(properties.getRag().getDocumentsPath());
            if (!Files.exists(documentsPath)) {
                indexed.set(false);
                return ReindexResponse.builder()
                    .success(false)
                    .indexedDocumentCount(0)
                    .message("Knowledge directory does not exist: " + documentsPath)
                    .build();
            }
            List<Document> documents = properties.getRag().isRecursive()
                ? FileSystemDocumentLoader.loadDocumentsRecursively(documentsPath)
                : FileSystemDocumentLoader.loadDocuments(documentsPath);

            DocumentSplitter splitter = DocumentSplitters.recursive(
                properties.getRag().getMaxSegmentSize(),
                properties.getRag().getMaxOverlapSize()
            );
            List<TextSegment> allSegments = new ArrayList<>();
            for (Document document : documents) {
                List<TextSegment> segments = splitter.split(document);
                for (TextSegment segment : segments) {
                    // 每个分段都保留来源元数据，后续回答时才能回传可用的引用信息。
                    segment.metadata().put("source", document.metadata().getString("source"));
                    segment.metadata().put("title", titleFor(document));
                }
                allSegments.addAll(segments);
            }
            List<Embedding> embeddings = embeddingModel.embedAll(allSegments).content();
            if (embeddingStore instanceof InMemoryEmbeddingStore<TextSegment> inMemoryStore) {
                inMemoryStore.removeAll();
            }
            embeddingStore.addAll(embeddings, allSegments);
            indexed.set(true);
            return ReindexResponse.builder()
                .success(true)
                .indexedDocumentCount(allSegments.size())
                .message("Knowledge base indexed successfully.")
                .build();
        } catch (Exception exception) {
            indexed.set(false);
            return ReindexResponse.builder()
                .success(false)
                .indexedDocumentCount(0)
                .message("Knowledge base indexing failed: " + exception.getMessage())
                .build();
        }
    }

    public String formatForPrompt(List<KnowledgeSnippet> snippets) {
        if (snippets.isEmpty()) {
            return "No knowledge context available.";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < snippets.size(); i++) {
            KnowledgeSnippet snippet = snippets.get(i);
            builder.append(i + 1)
                .append(". [")
                .append(snippet.title())
                .append("] ")
                .append(snippet.text())
                .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private boolean isSearchReady() {
        return embeddingModel != null && embeddingStore != null;
    }

    private void initializeInfrastructure() {
        if (embeddingStore == null) {
            embeddingStore = buildEmbeddingStore();
        }
        if (embeddingModel == null) {
            embeddingModel = buildEmbeddingModel();
        }
    }

    private EmbeddingStore<TextSegment> buildEmbeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    private EmbeddingModel buildEmbeddingModel() {
        if (!properties.getEmbedding().isEnabled()
            || !StringUtils.hasText(properties.getEmbedding().getBaseUrl())
            || !StringUtils.hasText(properties.getEmbedding().getApiKey())
            || !StringUtils.hasText(properties.getEmbedding().getModelName())) {
            return null;
        }
        return OpenAiEmbeddingModel.builder()
            .baseUrl(properties.getEmbedding().getBaseUrl())
            .apiKey(properties.getEmbedding().getApiKey())
            .modelName(properties.getEmbedding().getModelName())
            .timeout(timeout(properties.getEmbedding().getTimeout()))
            .build();
    }

    private Duration timeout(Duration duration) {
        return duration == null ? Duration.ofSeconds(30) : duration;
    }

    private String titleFor(Document document) {
        String source = document.metadata().getString("source");
        if (!StringUtils.hasText(source)) {
            return "knowledge";
        }
        return Path.of(source).getFileName().toString();
    }
}
