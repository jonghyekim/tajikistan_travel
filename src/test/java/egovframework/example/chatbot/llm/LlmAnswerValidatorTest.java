package egovframework.example.chatbot.llm;

import egovframework.example.chatbot.domain.SourceType;
import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.rag.RagContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmAnswerValidatorTest {

    private final LlmAnswerValidator validator = new LlmAnswerValidator();

    @Test
    void rejectsUnknownSourceIds() {
        RagContext context = new RagContext(
            List.of(new RagContext.SourceDocument("tour_place:1", SourceType.TOUR_PLACE, "Museum", "Museum in Dushanbe")),
            "[tour_place:1] Museum in Dushanbe"
        );

        GroundedAnswer answer = GroundedAnswer.llm("Museum in Dushanbe", List.of("tour_place:999"));

        assertThat(validator.isValid(answer, context)).isFalse();
    }

    @Test
    void acceptsKnownSourceIds() {
        RagContext context = new RagContext(
            List.of(new RagContext.SourceDocument("tour_place:1", SourceType.TOUR_PLACE, "Museum", "Museum in Dushanbe")),
            "[tour_place:1] Museum in Dushanbe"
        );

        GroundedAnswer answer = GroundedAnswer.llm("Museum in Dushanbe", List.of("tour_place:1"));

        assertThat(validator.isValid(answer, context)).isTrue();
    }
}
