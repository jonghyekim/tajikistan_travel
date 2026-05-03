package egovframework.example.chatbot.rag;

import egovframework.example.chatbot.dto.OperatingHourFact;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RagContextBuilderTest {

    private final RagContextBuilder builder = new RagContextBuilder();

    @Test
    void operatingHoursAcceptDailyRowsWithoutDayOfWeek() {
        OperatingHourFact hour = new OperatingHourFact(
            26L,
            26L,
            "operating_hour:26",
            "Боғи Абуабдулло Рудакӣ",
            null,
            "ALL",
            LocalTime.MIDNIGHT,
            LocalTime.of(23, 59),
            false,
            null,
            null,
            "tg"
        );

        assertThatCode(() -> builder.fromOperatingHours(List.of(hour))).doesNotThrowAnyException();

        RagContext context = builder.fromOperatingHours(List.of(hour));
        assertThat(context.text()).contains("ALL");
        assertThat(context.sources()).hasSize(1);
    }
}
