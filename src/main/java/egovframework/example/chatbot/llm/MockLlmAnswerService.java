package egovframework.example.chatbot.llm;

import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.IntentResult;
import egovframework.example.chatbot.rag.RagContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "chatbot.llm.mode", havingValue = "mock", matchIfMissing = true)
public class MockLlmAnswerService implements LlmAnswerService {

    @Override
    public GroundedAnswer generate(String question, IntentResult intent, RagContext context) {
        String answer = "%s\n%s".formatted(prefix(intent.locale()), context.text());
        return GroundedAnswer.llm(answer.trim(), context.sourceIds());
    }

    private String prefix(String locale) {
        return switch (locale == null ? "en" : locale) {
            case "ko" -> "아래 DB 근거를 기준으로 안내합니다.";
            case "ru" -> "Ответ основан только на данных из базы данных.";
            case "tg" -> "Ҷавоб танҳо дар асоси маълумоти пойгоҳи додаҳо дода мешавад.";
            default -> "This answer is based only on verified database context.";
        };
    }
}
