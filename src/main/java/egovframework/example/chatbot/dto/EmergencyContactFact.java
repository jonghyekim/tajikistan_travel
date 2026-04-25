package egovframework.example.chatbot.dto;

import java.io.Serializable;

public record EmergencyContactFact(
    Long contactId,
    String sourceId,
    String code,
    String title,
    String description,
    String phoneDial,
    String phoneDisplay,
    String badgeType,
    String badgeLabel,
    String locale
) implements Serializable {
}
