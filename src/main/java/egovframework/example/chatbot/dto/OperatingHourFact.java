package egovframework.example.chatbot.dto;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record OperatingHourFact(
    Long operatingHourId,
    Long placeId,
    String sourceId,
    String placeTitle,
    DayOfWeek dayOfWeek,
    String seasonCode,
    LocalTime opensAt,
    LocalTime closesAt,
    boolean closed,
    LocalTime lastAdmissionAt,
    String note,
    String locale
) implements Serializable {
}
