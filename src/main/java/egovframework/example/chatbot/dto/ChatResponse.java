package egovframework.example.chatbot.dto;

import java.io.Serializable;
import java.util.List;

public record ChatResponse(
    String answer,
    String intent,
    boolean grounded,
    boolean llmUsed,
    boolean noData,
    List<String> sourceIds,
    List<Citation> citations
) implements Serializable {

    public static ChatResponse noData(String intent, String locale) {
        return new ChatResponse(
            noDataMessage(locale),
            intent,
            true,
            false,
            true,
            List.of(),
            List.of()
        );
    }

    public static ChatResponse noData(String intent) {
        return noData(intent, "en");
    }

    private static String noDataMessage(String locale) {
        return switch (locale == null ? "en" : locale) {
            case "ko" -> "DB에서 확인된 정보가 없습니다.";
            case "ru" -> "В базе данных нет подтвержденной информации.";
            case "tg" -> "Дар пойгоҳи додаҳо маълумоти тасдиқшуда ёфт нашуд.";
            default -> "No verified information was found in the database.";
        };
    }

    public record Citation(
        String sourceId,
        String sourceType,
        String title
    ) implements Serializable {
    }
}
