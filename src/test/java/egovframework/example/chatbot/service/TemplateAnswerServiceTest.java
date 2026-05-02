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
        assertThat(answer.answer()).doesNotContain("Hotel Atlas is located");
        assertThat(answer.answer()).doesNotContain("[tour_place:80]");
        assertThat(answer.sourceIds()).containsExactly("tour_place:80");
    }
}
