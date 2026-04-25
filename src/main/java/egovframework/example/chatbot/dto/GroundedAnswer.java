package egovframework.example.chatbot.dto;

import java.io.Serializable;
import java.util.List;

public record GroundedAnswer(
    String answer,
    List<String> sourceIds,
    boolean grounded,
    boolean llmUsed
) implements Serializable {

    public static GroundedAnswer template(String answer, List<String> sourceIds) {
        return new GroundedAnswer(answer, sourceIds, true, false);
    }

    public static GroundedAnswer llm(String answer, List<String> sourceIds) {
        return new GroundedAnswer(answer, sourceIds, true, true);
    }
}
