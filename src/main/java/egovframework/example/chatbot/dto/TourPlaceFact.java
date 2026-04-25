package egovframework.example.chatbot.dto;

import java.io.Serializable;

public record TourPlaceFact(
    Long placeId,
    String sourceId,
    String title,
    String content,
    String address,
    String categoryCode,
    String regionCode,
    String locale
) implements Serializable {
}
