package egovframework.example.chatbot.service;

import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateAnswerService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public GroundedAnswer operatingHours(List<OperatingHourFact> hours) {
        Map<String, List<OperatingHourFact>> byPlace = hours.stream()
            .collect(Collectors.groupingBy(OperatingHourFact::placeTitle, java.util.LinkedHashMap::new, Collectors.toList()));

        String answer = byPlace.entrySet().stream()
            .map(entry -> entry.getKey() + "\n" + entry.getValue().stream()
                .map(this::formatOperatingHour)
                .collect(Collectors.joining("\n")))
            .collect(Collectors.joining("\n\n"));

        return GroundedAnswer.template(answer, hours.stream().map(OperatingHourFact::sourceId).toList());
    }

    public GroundedAnswer emergencyContacts(List<EmergencyContactFact> contacts) {
        String answer = contacts.stream()
            .map(contact -> contact.title() + ": " + contact.phoneDisplay() + " (" + contact.description() + ")")
            .collect(Collectors.joining("\n"));
        return GroundedAnswer.template(answer, contacts.stream().map(EmergencyContactFact::sourceId).toList());
    }

    public GroundedAnswer phoneNumbers(List<EmergencyContactFact> contacts) {
        String answer = contacts.stream()
            .map(contact -> contact.title() + ": " + contact.phoneDisplay())
            .collect(Collectors.joining("\n"));
        return GroundedAnswer.template(answer, contacts.stream().map(EmergencyContactFact::sourceId).toList());
    }

    public GroundedAnswer tourPlaces(List<TourPlaceFact> places) {
        String answer = places.stream()
            .map(place -> "- " + place.title() + " " + detailPageGuide(place.locale()))
            .collect(Collectors.joining("\n"));
        return GroundedAnswer.template(answer, places.stream().map(TourPlaceFact::sourceId).toList());
    }

    private String detailPageGuide(String locale) {
        return switch (locale == null ? "en" : locale) {
            case "ko" -> "상세 페이지에서 위치와 상세 정보를 확인해 주세요.";
            case "ru" -> "Откройте страницу деталей, чтобы проверить расположение и подробную информацию.";
            case "tg" -> "Барои дидани ҷойгиршавӣ ва маълумоти муфассал саҳифаи тафсилотро кушоед.";
            default -> "Open the detail page to check the location and details.";
        };
    }

    private String formatOperatingHour(OperatingHourFact hour) {
        if (hour.closed()) {
            return "- " + dayName(hour) + ": " + closedLabel(hour.locale()) + note(hour.note());
        }
        String range = format(hour.opensAt()) + "-" + format(hour.closesAt());
        String lastAdmission = hour.lastAdmissionAt() == null ? "" : ", " + lastAdmissionLabel(hour.locale()) + " " + format(hour.lastAdmissionAt());
        return "- " + dayName(hour) + ": " + range + lastAdmission + note(hour.note());
    }

    private String format(java.time.LocalTime time) {
        return time == null ? "N/A" : time.format(TIME);
    }

    private String note(String note) {
        return note == null || note.isBlank() ? "" : " (" + note + ")";
    }

    private String dayName(OperatingHourFact hour) {
        if (hour.dayOfWeek() == null) {
            return switch (hour.locale() == null ? "en" : hour.locale()) {
                case "ko" -> "매일";
                case "ru" -> "Ежедневно";
                case "tg" -> "Ҳар рӯз";
                default -> "Daily";
            };
        }
        return switch (hour.locale() == null ? "en" : hour.locale()) {
            case "ko" -> switch (hour.dayOfWeek()) {
                case MONDAY -> "월요일";
                case TUESDAY -> "화요일";
                case WEDNESDAY -> "수요일";
                case THURSDAY -> "목요일";
                case FRIDAY -> "금요일";
                case SATURDAY -> "토요일";
                case SUNDAY -> "일요일";
            };
            case "ru" -> switch (hour.dayOfWeek()) {
                case MONDAY -> "Понедельник";
                case TUESDAY -> "Вторник";
                case WEDNESDAY -> "Среда";
                case THURSDAY -> "Четверг";
                case FRIDAY -> "Пятница";
                case SATURDAY -> "Суббота";
                case SUNDAY -> "Воскресенье";
            };
            case "tg" -> switch (hour.dayOfWeek()) {
                case MONDAY -> "Душанбе";
                case TUESDAY -> "Сешанбе";
                case WEDNESDAY -> "Чоршанбе";
                case THURSDAY -> "Панҷшанбе";
                case FRIDAY -> "Ҷумъа";
                case SATURDAY -> "Шанбе";
                case SUNDAY -> "Якшанбе";
            };
            default -> hour.dayOfWeek().name();
        };
    }

    private String closedLabel(String locale) {
        return switch (locale == null ? "en" : locale) {
            case "ko" -> "휴무";
            case "ru" -> "закрыто";
            case "tg" -> "баста";
            default -> "closed";
        };
    }

    private String lastAdmissionLabel(String locale) {
        return switch (locale == null ? "en" : locale) {
            case "ko" -> "마지막 입장";
            case "ru" -> "последний вход";
            case "tg" -> "воридшавии охирин";
            default -> "last admission";
        };
    }
}
