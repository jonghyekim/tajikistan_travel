package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.AnswerType;
import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.domain.SearchPlan;
import egovframework.example.chatbot.dto.ChatRoute;
import egovframework.example.chatbot.dto.ChatRequest;
import egovframework.example.chatbot.dto.ChatResponse;
import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import egovframework.example.chatbot.llm.LlmAnswerService;
import egovframework.example.chatbot.llm.LlmAnswerValidator;
import egovframework.example.chatbot.rag.RagContextBuilder;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatApplicationServiceTest {

    private final MessageNormalizer messageNormalizer = new MessageNormalizer();
    private final IntentClassifier intentClassifier = new IntentClassifier();
    private final ChatRouterService chatRouterService = new RuleBasedChatRouterService(intentClassifier);
    private final TourPlaceQueryService tourPlaceQueryService = mock(TourPlaceQueryService.class);
    private final EmergencyContactQueryService emergencyContactQueryService = mock(EmergencyContactQueryService.class);
    private final LlmAnswerService llmAnswerService = mock(LlmAnswerService.class);

    private final ChatApplicationService chatApplicationService = new ChatApplicationService(
        messageNormalizer,
        intentClassifier,
        chatRouterService,
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
    void emergencyContactUsesValidatedLlmAnswer() {
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
        when(llmAnswerService.generate(any(), any(), any()))
            .thenReturn(GroundedAnswer.llm("Police emergency number is 102.", List.of("emergency_contact:1")));

        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "emergency police phone number",
            "en",
            null,
            Map.of()
        ));

        assertThat(response.intent()).isEqualTo(ChatIntent.EMERGENCY_CONTACT.name());
        assertThat(response.llmUsed()).isTrue();
        assertThat(response.answer()).contains("Police").contains("102");
        assertThat(response.sourceIds()).containsExactly("emergency_contact:1");
        verify(llmAnswerService).generate(any(), any(), any());
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
        verify(llmAnswerService).generate(any(), any(), any());
    }

    @Test
    void genericEmergencyRequestFetchesAllContactsWithoutKeywordFilter() {
        when(emergencyContactQueryService.findActiveContacts(isNull(), any())).thenReturn(List.of(
            new EmergencyContactFact(
                1L,
                "emergency_contact:1",
                "general",
                "일반 긴급",
                "통합 긴급 전화",
                "112",
                "112",
                "emergency",
                "긴급",
                "ko"
            )
        ));
        when(llmAnswerService.generate(any(), any(), any()))
            .thenReturn(GroundedAnswer.llm("긴급 상황에서는 112로 연락하세요.", List.of("emergency_contact:1")));

        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "긴급상황. 연락처 필요",
            "ko",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isFalse();
        assertThat(response.intent()).isEqualTo(ChatIntent.EMERGENCY_CONTACT.name());
        assertThat(response.sourceIds()).containsExactly("emergency_contact:1");
        verify(emergencyContactQueryService).findActiveContacts(isNull(), any());
    }

    @Test
    void locationQuestionWithoutPlaceNameDoesNotSearchAllPlaces() {
        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "위치가 어디야",
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
    void usesServerTemplateEvenWhenValidLlmSourceContainsUnsupportedDetails() {
        when(tourPlaceQueryService.searchPlaces(any(), any())).thenReturn(List.of(
            new TourPlaceFact(
                10L,
                "tour_place:10",
                "Rudaki Park",
                "Central park in Dushanbe.",
                "Dushanbe",
                "PARK",
                "DUSHANBE",
                "en"
            )
        ));
        when(llmAnswerService.generate(any(), any(), any()))
            .thenReturn(GroundedAnswer.llm("Rudaki Park has a free airport shuttle and costs $20.", List.of("tour_place:10")));

        ChatResponse response = chatApplicationService.chat(new ChatRequest(
            "Rudaki Park location",
            "en",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isFalse();
        assertThat(response.llmUsed()).isTrue();
        assertThat(response.answer()).contains("Open the detail page");
        assertThat(response.answer()).doesNotContain("airport shuttle", "$20");
        assertThat(response.sourceIds()).containsExactly("tour_place:10");
    }

    @Test
    void aiCanChooseAnswerSourceTypeFromMixedCandidates() {
        ChatApplicationService routedChatService = new ChatApplicationService(
            messageNormalizer,
            intentClassifier,
            (question, normalizedMessage, locale) -> new ChatRoute(
                ChatIntent.OPERATING_HOURS,
                SearchPlan.OPERATING_HOURS_BY_PLACE,
                "Rudaki Park",
                null,
                0.95
            ),
            tourPlaceQueryService,
            emergencyContactQueryService,
            new RagContextBuilder(),
            new TemplateAnswerService(),
            llmAnswerService,
            new LlmAnswerValidator()
        );
        when(tourPlaceQueryService.searchPlaces(any(), any())).thenReturn(List.of(
            new TourPlaceFact(
                10L,
                "tour_place:10",
                "Rudaki Park",
                "Central park in Dushanbe.",
                "Dushanbe",
                "PARK",
                "DUSHANBE",
                "en"
            )
        ));
        when(tourPlaceQueryService.findOperatingHours(any(), any())).thenReturn(List.of(
            new OperatingHourFact(
                20L,
                10L,
                "operating_hour:20",
                "Rudaki Park",
                DayOfWeek.MONDAY,
                "ALL",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                false,
                null,
                null,
                "en"
            )
        ));
        when(llmAnswerService.generate(any(), any(), any()))
            .thenReturn(GroundedAnswer.llm("Rudaki Park is open from 09:00 to 18:00 on Monday.", List.of("operating_hour:20")));

        ChatResponse response = routedChatService.chat(new ChatRequest(
            "Rudaki Park",
            "en",
            null,
            Map.of()
        ));

        assertThat(response.intent()).isEqualTo(ChatIntent.OPERATING_HOURS.name());
        assertThat(response.llmUsed()).isTrue();
        assertThat(response.sourceIds()).containsExactly("operating_hour:20");
        assertThat(response.citations()).extracting(ChatResponse.Citation::sourceType).containsExactly("OPERATING_HOUR");
    }

    @Test
    void recordsConversationLogForNoDataResponse() {
        ChatbotConversationLogService conversationLogService = mock(ChatbotConversationLogService.class);
        ChatApplicationService loggingChatService = new ChatApplicationService(
            messageNormalizer,
            intentClassifier,
            chatRouterService,
            tourPlaceQueryService,
            emergencyContactQueryService,
            new RagContextBuilder(),
            new TemplateAnswerService(),
            llmAnswerService,
            new LlmAnswerValidator(),
            conversationLogService
        );

        ChatResponse response = loggingChatService.chat(new ChatRequest(
            "위치가 어디야",
            "ko",
            null,
            Map.of()
        ));

        assertThat(response.noData()).isTrue();
        verify(conversationLogService).record(
            eq("위치가 어디야"),
            eq("위치가 어디야"),
            eq("ko"),
            any(ChatRoute.class),
            eq(AnswerType.NO_DATA),
            eq(response),
            anyLong()
        );
    }
}
