package egovframework.example.chatbot.dto;

import egovframework.example.chatbot.domain.ChatIntent;

import java.io.Serializable;
import java.util.Map;

public record IntentResult(
    ChatIntent intent,
    double confidence,
    String normalizedMessage,
    String keyword,
    String locale,
    Map<String, String> slots,
    boolean exactInformationIntent
) implements Serializable {
}
