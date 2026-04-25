package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.dto.ChatRequest;
import egovframework.example.chatbot.dto.ChatResponse;
import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.llm.LlmAnswerService;
import egovframework.example.chatbot.llm.LlmAnswerValidator;
import egovframework.example.chatbot.rag.RagContextBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatApplicationServiceTest {

    private final MessageNormalizer messageNormalizer = new MessageNormalizer();
    private final IntentClassifier intentClassifier = new IntentClassifier();
    private final TourPlaceQueryService tourPlaceQueryService = mock(TourPlaceQueryService.class);
    private final EmergencyContactQueryService emergencyContactQueryService = mock(EmergencyContactQueryService.class);
    private final LlmAnswerService llmAnswerService = mock(LlmAnswerService.class);

    private final ChatApplicationService chatApplicationService = new ChatApplicationService(
        messageNormalizer,
        intentClassifier,
        tourPlaceQueryService,
        emergencyContactQueryService,
        new RagContextBuilder(),
        new TemplateAnswerService(),
        llmAnswerService,
        new LlmAnswerValidator()
    );

    @Test
    void doesNotCallLlmWhenDbReturnsNoData() {
        when(tourPlaceQueryService.searchPlaces(any(), any())).thenReturn(List.of());

        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "recommend tourist places",
            "en",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isTrue();
        verify(llmAnswerService, never()).generate(any(), any(), any());
    }

    @Test
    void generalTourismDoesNotFallThroughToPlaceSearch() {
        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "야",
            "ko",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isTrue();
        assertThat(response.intent()).isEqualTo(ChatIntent.GENERAL_TOURISM.name());
        verify(tourPlaceQueryService, never()).searchPlaces(any(), any());
        verify(llmAnswerService, never()).generate(any(), any(), any());
    }

    @Test
    void genericTourPlaceIntentWithoutKeywordDoesNotSearchAllPlaces() {
        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "추천",
            "ko",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isTrue();
        assertThat(response.intent()).isEqualTo(ChatIntent.TOUR_PLACE_SEARCH.name());
        verify(tourPlaceQueryService, never()).searchPlaces(any(), any());
        verify(llmAnswerService, never()).generate(any(), any(), any());
    }

    @Test
    void operatingHoursWithoutPlaceKeywordDoesNotSearchAllHours() {
        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "시간",
            "ko",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isTrue();
        assertThat(response.intent()).isEqualTo(ChatIntent.OPERATING_HOURS.name());
        verify(tourPlaceQueryService, never()).findOperatingHours(any(), any());
    }

    @Test
    void emergencyContactUsesTemplateWithoutLlm() {
        when(emergencyContactQueryService.findActiveContacts(any(), any())).thenReturn(List.of(
            new EmergencyContactFact(
                1L,
                "emergency_contact:1",
                "police",
                "Police",
                "Emergency police assistance",
                "102",
                "102",
                "police",
                "Police",
                "en"
            )
        ));

        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "emergency police phone number",
            "en",
            null,
            Map.of()
        ));

        assertThat(response.intent()).isEqualTo(ChatIntent.EMERGENCY_CONTACT.name());
        assertThat(response.llmUsed()).isFalse();
        assertThat(response.answer()).contains("Police").contains("102");
        assertThat(response.sourceIds()).containsExactly("emergency_contact:1");
        verify(llmAnswerService, never()).generate(any(), any(), any());
    }

    @Test
    void emergencyContactFallsBackWhenLookupFails() {
        when(emergencyContactQueryService.findActiveContacts(any(), any()))
            .thenThrow(new IllegalStateException("database unavailable"));

        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "경찰번호좀",
            "ko",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isFalse();
        assertThat(response.intent()).isEqualTo(ChatIntent.EMERGENCY_CONTACT.name());
        assertThat(response.answer()).contains("경찰").contains("102");
        assertThat(response.sourceIds()).containsExactly("emergency_contact:2");
        verify(llmAnswerService, never()).generate(any(), any(), any());
    }
}
