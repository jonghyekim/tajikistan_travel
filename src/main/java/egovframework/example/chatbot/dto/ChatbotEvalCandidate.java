package egovframework.example.chatbot.dto;

import java.time.LocalDateTime;

public record ChatbotEvalCandidate(
    Long logId,
    String message,
    String normalizedMessage,
    String locale,
    String intent,
    String searchPlan,
    String keyword,
    String contactType,
    String answerType,
    String sourceIds,
    String reason,
    LocalDateTime createdAt
) {
}
