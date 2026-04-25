package egovframework.example.chatbot.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class MessageNormalizer {

    public String normalize(String message) {
        if (message == null) {
            return "";
        }
        return Normalizer.normalize(message, Normalizer.Form.NFKC)
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ko", "en", "ru", "tg" -> normalized;
            default -> "en";
        };
    }
}
