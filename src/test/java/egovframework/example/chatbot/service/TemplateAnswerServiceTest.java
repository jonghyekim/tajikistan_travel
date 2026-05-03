package egovframework.example.chatbot.service;

import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateAnswerServiceTest {

    private final TemplateAnswerService templateAnswerService = new TemplateAnswerService();

    @Test
    void rendersOperatingHoursFromDbValues() {
        GroundedAnswer answer = templateAnswerService.operatingHours(List.of(
            new OperatingHourFact(
                10L,
                1L,
                "operating_hour:10",
                "National Museum",
                DayOfWeek.TUESDAY,
                "ALL",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                false,
                LocalTime.of(17, 30),
                "Ticket office closes earlier",
                "en"
            )
        ));

        assertThat(answer.llmUsed()).isFalse();
        assertThat(answer.answer()).contains("National Museum", "TUESDAY", "09:00-18:00", "17:30");
        assertThat(answer.sourceIds()).containsExactly("operating_hour:10");
    }

    @Test
    void operatingHoursKeepSameFactsAcrossLocales() {
        for (String locale : List.of("ko", "en", "ru", "tg")) {
            GroundedAnswer answer = templateAnswerService.operatingHours(List.of(
                new OperatingHourFact(
                    26L,
                    26L,
                    "operating_hour:26",
                    placeTitle(locale),
                    null,
                    "ALL",
                    LocalTime.MIDNIGHT,
                    LocalTime.of(23, 59),
                    false,
                    null,
                    null,
                    locale
                )
            ));

            assertThat(answer.answer()).contains("00:00-23:59");
            assertThat(answer.sourceIds()).containsExactly("operating_hour:26");
        }
    }

    @Test
    void rendersTourPlacesAsReadableFallbackList() {
        GroundedAnswer answer = templateAnswerService.tourPlaces(List.of(
            new TourPlaceFact(
                80L,
                "tour_place:80",
                "Atlas Hotel 4*",
                "Hotel Atlas is located in Dushanbe with free Wi-Fi and an indoor pool.",
                null,
                "STAY",
                "DUSHANBE",
                "en"
            )
        ));

        assertThat(answer.llmUsed()).isFalse();
        assertThat(answer.answer()).contains("- Atlas Hotel 4*");
        assertThat(answer.answer()).contains("detail page");
        assertThat(answer.answer()).endsWith("Open the detail page to check the location and details.");
        assertThat(answer.answer()).doesNotContain("Hotel Atlas is located");
        assertThat(answer.answer()).doesNotContain("[tour_place:80]");
        assertThat(answer.sourceIds()).containsExactly("tour_place:80");
    }

    @Test
    void rendersTourPlaceDetailGuideOnceAfterMultiplePlaces() {
        GroundedAnswer answer = templateAnswerService.tourPlaces(List.of(
            place(1L, "Hilton Dushanbe 5*", "ko"),
            place(2L, "세레나 호텔 두샨베 5*", "ko"),
            place(3L, "하얏트 리젠시 두샨베 5*", "ko")
        ));

        assertThat(answer.answer()).isEqualTo("""
            - Hilton Dushanbe 5*
            - 세레나 호텔 두샨베 5*
            - 하얏트 리젠시 두샨베 5*
            상세 페이지에서 위치와 상세 정보를 확인해 주세요.""");
        assertThat(answer.sourceIds()).containsExactly("tour_place:1", "tour_place:2", "tour_place:3");
    }

    private TourPlaceFact place(Long id, String title, String locale) {
        return new TourPlaceFact(
            id,
            "tour_place:" + id,
            title,
            "Description should not appear in the answer.",
            null,
            "STAY",
            "DUSHANBE",
            locale
        );
    }

    private String placeTitle(String locale) {
        return switch (locale) {
            case "ko" -> "아부압둘로 루다키 공원";
            case "ru" -> "Парк Абуабдулло Рудаки";
            case "tg" -> "Боғи Абуабдулло Рудакӣ";
            default -> "Abuabdullo Rudaki Park";
        };
    }
}
