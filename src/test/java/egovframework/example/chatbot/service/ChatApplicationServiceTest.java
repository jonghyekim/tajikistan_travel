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
    void operatingHourFactsStayStableAcrossLocalesEvenWhenLlmWordingDiffers() {
        for (String locale : List.of("ko", "en", "ru", "tg")) {
            TourPlaceQueryService places = mock(TourPlaceQueryService.class);
            EmergencyContactQueryService contacts = mock(EmergencyContactQueryService.class);
            LlmAnswerService llm = mock(LlmAnswerService.class);
            ChatApplicationService service = new ChatApplicationService(
                messageNormalizer,
                intentClassifier,
                (question, normalizedMessage, requestLocale) -> new ChatRoute(
                    ChatIntent.OPERATING_HOURS,
                    SearchPlan.OPERATING_HOURS_BY_PLACE,
                    placeTitle(locale),
                    null,
                    0.95
                ),
                places,
                contacts,
                new RagContextBuilder(),
                new TemplateAnswerService(),
                llm,
                new LlmAnswerValidator()
            );
            when(places.findOperatingHours(any(), any())).thenReturn(List.of(
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
            when(llm.generate(any(), any(), any()))
                .thenReturn(GroundedAnswer.llm("LLM wording for " + locale + " should not be used.", List.of("operating_hour:26")));

            ChatResponse response = service.chat(new ChatRequest(hoursQuestion(locale), locale, null, Map.of()));

            assertThat(response.noData()).isFalse();
            assertThat(response.intent()).isEqualTo(ChatIntent.OPERATING_HOURS.name());
            assertThat(response.answer()).contains(placeTitle(locale));
            assertThat(response.answer()).contains("00:00-23:59");
            assertThat(response.answer()).doesNotContain("LLM wording");
            assertThat(response.sourceIds()).containsExactly("operating_hour:26");
            assertThat(response.citations()).extracting(ChatResponse.Citation::sourceId).containsExactly("operating_hour:26");
        }
    }

    @Test
    void tourPlaceSourcesStayStableAcrossLocalesEvenWhenLlmWordingDiffers() {
        for (String locale : List.of("ko", "en", "ru", "tg")) {
            TourPlaceQueryService places = mock(TourPlaceQueryService.class);
            EmergencyContactQueryService contacts = mock(EmergencyContactQueryService.class);
            LlmAnswerService llm = mock(LlmAnswerService.class);
            ChatApplicationService service = new ChatApplicationService(
                messageNormalizer,
                intentClassifier,
                (question, normalizedMessage, requestLocale) -> new ChatRoute(
                    ChatIntent.TOUR_PLACE_SEARCH,
                    SearchPlan.PLACE_RECOMMENDATION,
                    placeTitle(locale),
                    null,
                    0.95
                ),
                places,
                contacts,
                new RagContextBuilder(),
                new TemplateAnswerService(),
                llm,
                new LlmAnswerValidator()
            );
            when(places.searchPlaces(any(), any())).thenReturn(List.of(tourPlace(locale)));
            when(llm.generate(any(), any(), any()))
                .thenReturn(GroundedAnswer.llm("LLM says this place has free tickets, airport shuttle, and VIP access.", List.of("tour_place:26")));

            ChatResponse response = service.chat(new ChatRequest(placeQuestion(locale), locale, null, Map.of()));

            assertThat(response.noData()).isFalse();
            assertThat(response.intent()).isEqualTo(ChatIntent.TOUR_PLACE_SEARCH.name());
            assertThat(response.answer()).contains(placeTitle(locale));
            assertThat(response.answer()).doesNotContain("free tickets", "airport shuttle", "VIP access");
            assertThat(response.sourceIds()).containsExactly("tour_place:26");
            assertThat(response.citations()).extracting(ChatResponse.Citation::sourceId).containsExactly("tour_place:26");
            assertThat(response.citations()).extracting(ChatResponse.Citation::sourceType).containsExactly("TOUR_PLACE");
        }
    }

    @Test
    void emergencyPhoneFactsStayStableAcrossLocalesEvenWhenLlmWordingDiffers() {
        for (String locale : List.of("ko", "en", "ru", "tg")) {
            TourPlaceQueryService places = mock(TourPlaceQueryService.class);
            EmergencyContactQueryService contacts = mock(EmergencyContactQueryService.class);
            LlmAnswerService llm = mock(LlmAnswerService.class);
            ChatApplicationService service = new ChatApplicationService(
                messageNormalizer,
                intentClassifier,
                (question, normalizedMessage, requestLocale) -> new ChatRoute(
                    ChatIntent.PHONE_NUMBER,
                    SearchPlan.EMERGENCY_CONTACT_BY_TYPE,
                    "police",
                    "police",
                    0.95
                ),
                places,
                contacts,
                new RagContextBuilder(),
                new TemplateAnswerService(),
                llm,
                new LlmAnswerValidator()
            );
            when(contacts.findActiveContacts(any(), any())).thenReturn(List.of(policeContact(locale)));
            when(llm.generate(any(), any(), any()))
                .thenReturn(GroundedAnswer.llm("LLM tries to add 911 and 999, but selected the right source.", List.of("emergency_contact:2")));

            ChatResponse response = service.chat(new ChatRequest(policeQuestion(locale), locale, null, Map.of()));

            assertThat(response.noData()).isFalse();
            assertThat(response.intent()).isEqualTo(ChatIntent.PHONE_NUMBER.name());
            assertThat(response.answer()).contains(policeTitle(locale), "102");
            assertThat(response.answer()).doesNotContain("911", "999");
            assertThat(response.sourceIds()).containsExactly("emergency_contact:2");
            assertThat(response.citations()).extracting(ChatResponse.Citation::sourceId).containsExactly("emergency_contact:2");
            assertThat(response.citations()).extracting(ChatResponse.Citation::sourceType).containsExactly("EMERGENCY_CONTACT");
        }
    }

    @Test
    void noDataContractStaysStableAcrossLocales() {
        for (String locale : List.of("ko", "en", "ru", "tg")) {
            ChatApplicationService service = new ChatApplicationService(
                messageNormalizer,
                intentClassifier,
                (question, normalizedMessage, requestLocale) -> new ChatRoute(
                    ChatIntent.TOUR_PLACE_SEARCH,
                    SearchPlan.PLACE_BY_NAME,
                    "missing place",
                    null,
                    0.8
                ),
                mock(TourPlaceQueryService.class),
                mock(EmergencyContactQueryService.class),
                new RagContextBuilder(),
                new TemplateAnswerService(),
                mock(LlmAnswerService.class),
                new LlmAnswerValidator()
            );

            ChatResponse response = service.chat(new ChatRequest(missingPlaceQuestion(locale), locale, null, Map.of()));

            assertThat(response.noData()).isTrue();
            assertThat(response.grounded()).isTrue();
            assertThat(response.llmUsed()).isFalse();
            assertThat(response.intent()).isEqualTo(ChatIntent.TOUR_PLACE_SEARCH.name());
            assertThat(response.answer()).isEqualTo(noDataMessage(locale));
            assertThat(response.sourceIds()).isEmpty();
            assertThat(response.citations()).isEmpty();
        }
    }

    @Test
    void invalidLlmSourceIdFallsBackToVerifiedTemplateAcrossLocales() {
        for (String locale : List.of("ko", "en", "ru", "tg")) {
            TourPlaceQueryService places = mock(TourPlaceQueryService.class);
            EmergencyContactQueryService contacts = mock(EmergencyContactQueryService.class);
            LlmAnswerService llm = mock(LlmAnswerService.class);
            ChatApplicationService service = new ChatApplicationService(
                messageNormalizer,
                intentClassifier,
                (question, normalizedMessage, requestLocale) -> new ChatRoute(
                    ChatIntent.TOUR_PLACE_SEARCH,
                    SearchPlan.PLACE_BY_NAME,
                    placeTitle(locale),
                    null,
                    0.95
                ),
                places,
                contacts,
                new RagContextBuilder(),
                new TemplateAnswerService(),
                llm,
                new LlmAnswerValidator()
            );
            when(places.searchPlaces(any(), any())).thenReturn(List.of(tourPlace(locale)));
            when(llm.generate(any(), any(), any()))
                .thenReturn(GroundedAnswer.llm("Unsupported answer from invalid source.", List.of("tour_place:999")));

            ChatResponse response = service.chat(new ChatRequest(placeQuestion(locale), locale, null, Map.of()));

            assertThat(response.noData()).isFalse();
            assertThat(response.answer()).contains(placeTitle(locale));
            assertThat(response.answer()).doesNotContain("Unsupported answer");
            assertThat(response.sourceIds()).containsExactly("tour_place:26");
            assertThat(response.citations()).extracting(ChatResponse.Citation::sourceId).containsExactly("tour_place:26");
        }
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

    private String placeTitle(String locale) {
        return switch (locale) {
            case "ko" -> "아부압둘로 루다키 공원";
            case "ru" -> "Парк Абуабдулло Рудаки";
            case "tg" -> "Боғи Абуабдулло Рудакӣ";
            default -> "Abuabdullo Rudaki Park";
        };
    }

    private String hoursQuestion(String locale) {
        return switch (locale) {
            case "ko" -> "루다키 공원 운영 시간";
            case "ru" -> "часы работы парка Рудаки";
            case "tg" -> "Боғи Рӯдакӣ соатҳои кор";
            default -> "Rudaki Park opening hours";
        };
    }

    private TourPlaceFact tourPlace(String locale) {
        return new TourPlaceFact(
            26L,
            "tour_place:26",
            placeTitle(locale),
            "Verified database description that must not be exposed by the template.",
            "Dushanbe",
            "PARK",
            "DUSHANBE",
            locale
        );
    }

    private EmergencyContactFact policeContact(String locale) {
        return new EmergencyContactFact(
            2L,
            "emergency_contact:2",
            "police",
            policeTitle(locale),
            policeDescription(locale),
            "102",
            "102",
            "police",
            policeTitle(locale),
            locale
        );
    }

    private String policeTitle(String locale) {
        return switch (locale) {
            case "ko" -> "경찰";
            case "ru" -> "Полиция";
            case "tg" -> "Пулис";
            default -> "Police";
        };
    }

    private String policeDescription(String locale) {
        return switch (locale) {
            case "ko" -> "경찰 긴급 지원";
            case "ru" -> "Экстренная помощь полиции";
            case "tg" -> "Кумаки изтирории пулис";
            default -> "Emergency police assistance";
        };
    }

    private String placeQuestion(String locale) {
        return switch (locale) {
            case "ko" -> "루다키 공원 위치";
            case "ru" -> "где парк Рудаки";
            case "tg" -> "Боғи Рӯдакӣ дар куҷост";
            default -> "Rudaki Park location";
        };
    }

    private String policeQuestion(String locale) {
        return switch (locale) {
            case "ko" -> "경찰 번호 알려줘";
            case "ru" -> "номер полиции";
            case "tg" -> "рақами пулис";
            default -> "police phone number";
        };
    }

    private String missingPlaceQuestion(String locale) {
        return switch (locale) {
            case "ko" -> "없는장소 위치 알려줘";
            case "ru" -> "где неизвестное место";
            case "tg" -> "ҷойи номаълум дар куҷост";
            default -> "missing place location";
        };
    }

    private String noDataMessage(String locale) {
        return switch (locale) {
            case "ko" -> "DB에서 확인된 정보가 없습니다.";
            case "ru" -> "В базе данных нет подтвержденной информации.";
            case "tg" -> "Дар пойгоҳи додаҳо маълумоти тасдиқшуда ёфт нашуд.";
            default -> "No verified information was found in the database.";
        };
    }
}
