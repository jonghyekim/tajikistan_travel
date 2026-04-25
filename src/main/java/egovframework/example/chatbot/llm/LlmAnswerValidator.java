package egovframework.example.chatbot.llm;

import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.rag.RagContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class LlmAnswerValidator {

    public boolean isValid(GroundedAnswer answer, RagContext context) {
        if (answer == null || answer.answer() == null || answer.answer().isBlank()) {
            return false;
        }
        if (answer.sourceIds() == null || answer.sourceIds().isEmpty()) {
            return false;
        }
        Set<String> allowedSourceIds = new HashSet<>(context.sourceIds());
        if (!allowedSourceIds.containsAll(answer.sourceIds())) {
            return false;
        }
        return answer.sourceIds().stream().allMatch(sourceId ->
            context.sources().stream().anyMatch(source -> source.sourceId().equals(sourceId))
        );
    }
}
