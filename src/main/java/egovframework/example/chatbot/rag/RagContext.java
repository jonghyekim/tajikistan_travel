package egovframework.example.chatbot.rag;

import egovframework.example.chatbot.domain.SourceType;

import java.io.Serializable;
import java.util.List;

public record RagContext(
    List<SourceDocument> sources,
    String text
) implements Serializable {

    public boolean isEmpty() {
        return sources == null || sources.isEmpty();
    }

    public List<String> sourceIds() {
        return sources.stream().map(SourceDocument::sourceId).toList();
    }

    public record SourceDocument(
        String sourceId,
        SourceType sourceType,
        String title,
        String text
    ) implements Serializable {
    }
}
