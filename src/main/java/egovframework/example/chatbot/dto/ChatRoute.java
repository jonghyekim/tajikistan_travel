package egovframework.example.chatbot.dto;

import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.domain.SearchPlan;

import java.io.Serializable;

public record ChatRoute(
    ChatIntent intent,
    SearchPlan searchPlan,
    String keyword,
    String contactType,
    double confidence
) implements Serializable {

    public static ChatRoute unknown() {
        return new ChatRoute(ChatIntent.UNKNOWN, SearchPlan.NONE, null, null, 0.0);
    }
}
