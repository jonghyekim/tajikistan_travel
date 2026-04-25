package egovframework.example.chatbot.rag;

import egovframework.example.chatbot.domain.SourceType;
import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagContextBuilder {

    private static final int MAX_SOURCE_TEXT_LENGTH = 700;

    public RagContext fromTourPlaces(List<TourPlaceFact> places) {
        List<RagContext.SourceDocument> sources = places.stream()
            .map(place -> new RagContext.SourceDocument(
                place.sourceId(),
                SourceType.TOUR_PLACE,
                place.title(),
                compact(join(place.title(), place.address(), place.content(), place.categoryCode(), place.regionCode()))
            ))
            .toList();
        return new RagContext(sources, render(sources));
    }

    public RagContext fromOperatingHours(List<OperatingHourFact> hours) {
        List<RagContext.SourceDocument> sources = hours.stream()
            .map(hour -> new RagContext.SourceDocument(
                hour.sourceId(),
                SourceType.OPERATING_HOUR,
                hour.placeTitle(),
                compact(join(
                    hour.placeTitle(),
                    hour.dayOfWeek().name(),
                    hour.seasonCode(),
                    hour.closed() ? "closed" : hour.opensAt() + "-" + hour.closesAt(),
                    hour.lastAdmissionAt() == null ? null : "last admission " + hour.lastAdmissionAt(),
                    hour.note()
                ))
            ))
            .toList();
        return new RagContext(sources, render(sources));
    }

    public RagContext fromEmergencyContacts(List<EmergencyContactFact> contacts) {
        List<RagContext.SourceDocument> sources = contacts.stream()
            .map(contact -> new RagContext.SourceDocument(
                contact.sourceId(),
                SourceType.EMERGENCY_CONTACT,
                contact.title(),
                compact(join(contact.title(), contact.description(), contact.phoneDisplay(), contact.badgeLabel(), contact.code()))
            ))
            .toList();
        return new RagContext(sources, render(sources));
    }

    private String render(List<RagContext.SourceDocument> sources) {
        return sources.stream()
            .map(source -> "[" + source.sourceId() + "] " + source.text())
            .collect(Collectors.joining("\n"));
    }

    private String join(Object... values) {
        return java.util.Arrays.stream(values)
            .filter(value -> value != null && !value.toString().isBlank())
            .map(Object::toString)
            .collect(Collectors.joining(" | "));
    }

    private String compact(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_SOURCE_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_SOURCE_TEXT_LENGTH) + "...";
    }
}
