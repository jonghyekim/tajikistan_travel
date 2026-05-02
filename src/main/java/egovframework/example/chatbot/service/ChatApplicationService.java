package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.AnswerType;
import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.domain.SearchPlan;
import egovframework.example.chatbot.domain.SourceType;
import egovframework.example.chatbot.dto.ChatRequest;
import egovframework.example.chatbot.dto.ChatRoute;
import egovframework.example.chatbot.dto.ChatResponse;
import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.IntentResult;
import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import egovframework.example.chatbot.llm.LlmAnswerService;
import egovframework.example.chatbot.llm.LlmAnswerValidator;
import egovframework.example.chatbot.rag.RagContext;
import egovframework.example.chatbot.rag.RagContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);

    private final MessageNormalizer messageNormalizer;
    private final IntentClassifier intentClassifier;
    private final ChatRouterService chatRouterService;
    private final TourPlaceQueryService tourPlaceQueryService;
    private final EmergencyContactQueryService emergencyContactQueryService;
    private final RagContextBuilder ragContextBuilder;
    private final TemplateAnswerService templateAnswerService;
    private final LlmAnswerService llmAnswerService;
    private final LlmAnswerValidator llmAnswerValidator;
    private final ChatbotConversationLogService conversationLogService;

    @Autowired
    public ChatApplicationService(MessageNormalizer messageNormalizer,
                                  IntentClassifier intentClassifier,
                                  ChatRouterService chatRouterService,
                                  TourPlaceQueryService tourPlaceQueryService,
                                  EmergencyContactQueryService emergencyContactQueryService,
                                  RagContextBuilder ragContextBuilder,
                                  TemplateAnswerService templateAnswerService,
                                  LlmAnswerService llmAnswerService,
                                  LlmAnswerValidator llmAnswerValidator,
                                  ChatbotConversationLogService conversationLogService) {
        this.messageNormalizer = messageNormalizer;
        this.intentClassifier = intentClassifier;
        this.chatRouterService = chatRouterService;
        this.tourPlaceQueryService = tourPlaceQueryService;
        this.emergencyContactQueryService = emergencyContactQueryService;
        this.ragContextBuilder = ragContextBuilder;
        this.templateAnswerService = templateAnswerService;
        this.llmAnswerService = llmAnswerService;
        this.llmAnswerValidator = llmAnswerValidator;
        this.conversationLogService = conversationLogService;
    }

    public ChatApplicationService(MessageNormalizer messageNormalizer,
                                  IntentClassifier intentClassifier,
                                  ChatRouterService chatRouterService,
                                  TourPlaceQueryService tourPlaceQueryService,
                                  EmergencyContactQueryService emergencyContactQueryService,
                                  RagContextBuilder ragContextBuilder,
                                  TemplateAnswerService templateAnswerService,
                                  LlmAnswerService llmAnswerService,
                                  LlmAnswerValidator llmAnswerValidator) {
        this(
            messageNormalizer,
            intentClassifier,
            chatRouterService,
            tourPlaceQueryService,
            emergencyContactQueryService,
            ragContextBuilder,
            templateAnswerService,
            llmAnswerService,
            llmAnswerValidator,
            null
        );
    }

    public ChatResponse chat(ChatRequest request) {
        long startedAt = System.nanoTime();
        String normalizedMessage = messageNormalizer.normalize(request.message());
        String locale = messageNormalizer.normalizeLocale(request.locale());
        ChatRoute route = routeQuestion(request.message(), normalizedMessage, locale);
        IntentResult intent = toIntentResult(normalizedMessage, locale, route);

        ChatResponse response;
        AnswerType answerType;
        if (intent.intent() == ChatIntent.UNKNOWN || route.searchPlan() == SearchPlan.NONE) {
            response = ChatResponse.noData(intent.intent().name(), intent.locale());
            answerType = AnswerType.NO_DATA;
        } else {
            response = answerWithAiChoice(request.message(), normalizedMessage, intent, route);
            answerType = response.noData()
                ? AnswerType.NO_DATA
                : inferAnswerType(parseIntent(response.intent(), intent.intent()));
        }
        recordConversation(request, normalizedMessage, locale, route, answerType, response, startedAt);
        return response;
    }

    private ChatIntent parseIntent(String value, ChatIntent fallback) {
        try {
            return ChatIntent.valueOf(value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private void recordConversation(ChatRequest request,
                                    String normalizedMessage,
                                    String locale,
                                    ChatRoute route,
                                    AnswerType answerType,
                                    ChatResponse response,
                                    long startedAt) {
        if (conversationLogService == null) {
            return;
        }
        long responseMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        conversationLogService.record(
            request.message(),
            normalizedMessage,
            locale,
            route,
            answerType,
            response,
            responseMs
        );
    }

    private ChatRoute routeQuestion(String question, String normalizedMessage, String locale) {
        try {
            return normalizeRoute(chatRouterService.route(question, normalizedMessage, locale));
        } catch (RuntimeException ex) {
            log.warn("AI chat routing failed. Falling back to rule-based classifier.", ex);
            return normalizeRoute(ruleBasedRoute(normalizedMessage, locale));
        }
    }

    private ChatRoute ruleBasedRoute(String normalizedMessage, String locale) {
        IntentResult intent = intentClassifier.classify(normalizedMessage, locale);
        return new ChatRoute(
            intent.intent(),
            fallbackSearchPlan(intent),
            intent.keyword(),
            null,
            intent.confidence()
        );
    }

    private SearchPlan fallbackSearchPlan(IntentResult intent) {
        if (intent.intent() == ChatIntent.UNKNOWN) {
            return SearchPlan.NONE;
        }
        if (intent.keyword() == null || intent.keyword().isBlank()) {
            return switch (intent.intent()) {
                case EMERGENCY_CONTACT, PHONE_NUMBER -> SearchPlan.ALL_EMERGENCY_CONTACTS;
                default -> SearchPlan.NONE;
            };
        }
        return switch (intent.intent()) {
            case OPERATING_HOURS -> SearchPlan.OPERATING_HOURS_BY_PLACE;
            case EMERGENCY_CONTACT, PHONE_NUMBER -> SearchPlan.EMERGENCY_CONTACT_BY_TYPE;
            case TOUR_PLACE_SEARCH, GENERAL_TOURISM -> SearchPlan.PLACE_RECOMMENDATION;
            case UNKNOWN -> SearchPlan.NONE;
        };
    }

    private ChatRoute normalizeRoute(ChatRoute route) {
        if (route == null) {
            return ChatRoute.unknown();
        }
        ChatIntent intent = route.intent() == null ? ChatIntent.UNKNOWN : route.intent();
        SearchPlan searchPlan = route.searchPlan() == null ? SearchPlan.NONE : route.searchPlan();
        String keyword = route.keyword() == null || route.keyword().isBlank() ? null : route.keyword().trim();
        String contactType = route.contactType() == null || route.contactType().isBlank() ? null : route.contactType().trim();
        return new ChatRoute(intent, searchPlan, keyword, contactType, route.confidence());
    }

    private IntentResult toIntentResult(String normalizedMessage, String locale, ChatRoute route) {
        return new IntentResult(
            route.intent(),
            route.confidence(),
            normalizedMessage,
            route.keyword(),
            locale,
            java.util.Map.of("searchPlan", route.searchPlan().name()),
            route.intent() == ChatIntent.OPERATING_HOURS
                || route.intent() == ChatIntent.PHONE_NUMBER
                || route.intent() == ChatIntent.EMERGENCY_CONTACT
        );
    }

    private ChatResponse answerWithAiChoice(String question, String normalizedMessage, IntentResult intent, ChatRoute route) {
        CandidateAnswers candidates = collectCandidates(normalizedMessage, intent, route);
        if (candidates.context().isEmpty()) {
            return ChatResponse.noData(intent.intent().name(), intent.locale());
        }

        GroundedAnswer fallback = candidates.fallback();
        GroundedAnswer selectedAnswer = generateValidatedLlmAnswer(question, intent, candidates.context(), fallback);
        ChatIntent responseIntent = inferResponseIntent(intent, selectedAnswer, candidates.context());
        AnswerType answerType = inferAnswerType(responseIntent);
        GroundedAnswer answer = renderTemplateAnswer(answerType, selectedAnswer, candidates);
        return toResponse(responseIntent, intent.locale(), answer, candidates.context());
    }

    private CandidateAnswers collectCandidates(String normalizedMessage, IntentResult intent, ChatRoute route) {
        List<TourPlaceFact> places = List.of();
        List<OperatingHourFact> hours = List.of();
        List<EmergencyContactFact> contacts = List.of();
        String keyword = intent.keyword();

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword && shouldSearchTourPlaces(route.searchPlan())) {
            places = tourPlaceQueryService.searchPlaces(keyword, intent.locale());
        }

        if (hasKeyword && shouldSearchOperatingHours(route.searchPlan())) {
            hours = tourPlaceQueryService.findOperatingHours(keyword, intent.locale());
        }

        if (shouldSearchEmergencyContacts(normalizedMessage, intent, route.searchPlan())) {
            contacts = findEmergencyContactsWithFallback(intent, route);
        }

        RagContext context = mergeContexts(
            ragContextBuilder.fromTourPlaces(places),
            ragContextBuilder.fromOperatingHours(hours),
            ragContextBuilder.fromEmergencyContacts(contacts)
        );
        return new CandidateAnswers(
            places,
            hours,
            contacts,
            context,
            fallbackFor(intent, places, hours, contacts)
        );
    }

    private boolean shouldSearchTourPlaces(SearchPlan searchPlan) {
        return searchPlan == SearchPlan.PLACE_BY_NAME
            || searchPlan == SearchPlan.PLACE_RECOMMENDATION;
    }

    private boolean shouldSearchOperatingHours(SearchPlan searchPlan) {
        return searchPlan == SearchPlan.OPERATING_HOURS_BY_PLACE;
    }

    private boolean shouldSearchEmergencyContacts(String normalizedMessage, IntentResult intent, SearchPlan searchPlan) {
        return searchPlan == SearchPlan.ALL_EMERGENCY_CONTACTS
            || searchPlan == SearchPlan.EMERGENCY_CONTACT_BY_TYPE
            || intent.intent() == ChatIntent.EMERGENCY_CONTACT
            || intent.intent() == ChatIntent.PHONE_NUMBER
            || normalizedMessage.contains("emergency")
            || normalizedMessage.contains("긴급")
            || normalizedMessage.contains("응급")
            || normalizedMessage.contains("경찰")
            || normalizedMessage.contains("полиц")
            || normalizedMessage.contains("изтирор")
            || normalizedMessage.contains("пулис");
    }

    private List<EmergencyContactFact> findEmergencyContactsWithFallback(IntentResult intent, ChatRoute route) {
        String keyword = route.searchPlan() == SearchPlan.ALL_EMERGENCY_CONTACTS
            ? null
            : firstNonBlank(route.contactType(), route.keyword(), intent.keyword());
        IntentResult lookupIntent = new IntentResult(
            intent.intent(),
            intent.confidence(),
            intent.normalizedMessage(),
            keyword,
            intent.locale(),
            intent.slots(),
            intent.exactInformationIntent()
        );
        try {
            return emergencyContactQueryService.findActiveContacts(keyword, intent.locale());
        } catch (RuntimeException ex) {
            log.warn("Emergency contact lookup failed. keyword={}, locale={}", keyword, intent.locale(), ex);
            return fallbackEmergencyContacts(lookupIntent);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private GroundedAnswer fallbackFor(IntentResult intent,
                                       List<TourPlaceFact> places,
                                       List<OperatingHourFact> hours,
                                       List<EmergencyContactFact> contacts) {
        if (prefersEmergencyFallback(intent) && !contacts.isEmpty()) {
            return intent.intent() == ChatIntent.PHONE_NUMBER
                ? templateAnswerService.phoneNumbers(contacts)
                : templateAnswerService.emergencyContacts(contacts);
        }
        if (intent.intent() == ChatIntent.OPERATING_HOURS && !hours.isEmpty()) {
            return templateAnswerService.operatingHours(hours);
        }
        if (!places.isEmpty()) {
            return templateAnswerService.tourPlaces(places);
        }
        if (!hours.isEmpty()) {
            return templateAnswerService.operatingHours(hours);
        }
        if (contacts.isEmpty()) {
            return GroundedAnswer.template("", List.of());
        }
        return intent.intent() == ChatIntent.PHONE_NUMBER
            ? templateAnswerService.phoneNumbers(contacts)
            : templateAnswerService.emergencyContacts(contacts);
    }

    private boolean prefersEmergencyFallback(IntentResult intent) {
        return intent.intent() == ChatIntent.EMERGENCY_CONTACT || intent.intent() == ChatIntent.PHONE_NUMBER;
    }

    private RagContext mergeContexts(RagContext... contexts) {
        List<RagContext.SourceDocument> sources = new ArrayList<>();
        for (RagContext context : contexts) {
            if (context != null && context.sources() != null) {
                sources.addAll(context.sources());
            }
        }
        String text = sources.stream()
            .map(source -> "[" + source.sourceId() + "] " + source.text())
            .collect(java.util.stream.Collectors.joining("\n"));
        return new RagContext(sources, text);
    }

    private List<EmergencyContactFact> fallbackEmergencyContacts(IntentResult intent) {
        List<EmergencyContactFact> contacts = switch (intent.locale() == null ? "en" : intent.locale()) {
            case "ko" -> List.of(
                emergencyContact(1L, "general", "일반 긴급", "통합 긴급 전화 (경찰 / 구급 / 소방)", "112", "emergency", "긴급", "ko"),
                emergencyContact(2L, "police", "경찰", "경찰 긴급 지원", "102", "police", "경찰", "ko"),
                emergencyContact(3L, "ambulance", "구급차", "응급 의료 지원", "103", "medical", "구급", "ko"),
                emergencyContact(4L, "fire", "소방서", "화재 긴급 지원", "101", "fire", "소방", "ko")
            );
            case "ru" -> List.of(
                emergencyContact(1L, "general", "Общий экстренный номер", "Единый экстренный номер", "112", "emergency", "Экстренно", "ru"),
                emergencyContact(2L, "police", "Полиция", "Экстренная помощь полиции", "102", "police", "Полиция", "ru"),
                emergencyContact(3L, "ambulance", "Скорая помощь", "Экстренная медицинская помощь", "103", "medical", "Скорая", "ru"),
                emergencyContact(4L, "fire", "Пожарная служба", "Экстренная помощь при пожаре", "101", "fire", "Пожарная", "ru")
            );
            case "tg" -> List.of(
                emergencyContact(1L, "general", "Рақами умумии изтирорӣ", "Рақами ягонаи изтирорӣ", "112", "emergency", "Изтирорӣ", "tg"),
                emergencyContact(2L, "police", "Пулис", "Кумаки изтирории пулис", "102", "police", "Пулис", "tg"),
                emergencyContact(3L, "ambulance", "Ёрии таъҷилӣ", "Кумаки таъҷилии тиббӣ", "103", "medical", "Ёрии таъҷилӣ", "tg"),
                emergencyContact(4L, "fire", "Хадамоти оташнишонӣ", "Кумаки изтирорӣ ҳангоми сӯхтор", "101", "fire", "Оташнишонӣ", "tg")
            );
            default -> List.of(
                emergencyContact(1L, "general", "General Emergency", "Unified emergency number", "112", "emergency", "Emergency", "en"),
                emergencyContact(2L, "police", "Police", "Emergency police assistance", "102", "police", "Police", "en"),
                emergencyContact(3L, "ambulance", "Ambulance", "Emergency medical assistance", "103", "medical", "Ambulance", "en"),
                emergencyContact(4L, "fire", "Fire Service", "Emergency fire assistance", "101", "fire", "Fire", "en")
            );
        };

        String normalizedKeyword = intent.keyword() == null ? "" : intent.keyword().toLowerCase(java.util.Locale.ROOT);
        if (normalizedKeyword.contains("police") || normalizedKeyword.contains("경찰") || normalizedKeyword.contains("полиции") || normalizedKeyword.contains("пулис")) {
            return contacts.stream().filter(contact -> contact.code().equals("police")).toList();
        }
        if (normalizedKeyword.contains("ambulance") || normalizedKeyword.contains("응급") || normalizedKeyword.contains("구급") || normalizedKeyword.contains("ёрии") || normalizedKeyword.contains("таъҷилӣ")) {
            return contacts.stream().filter(contact -> contact.code().equals("ambulance")).toList();
        }
        if (normalizedKeyword.contains("fire") || normalizedKeyword.contains("소방") || normalizedKeyword.contains("пожар") || normalizedKeyword.contains("сӯхтор")) {
            return contacts.stream().filter(contact -> contact.code().equals("fire")).toList();
        }
        return contacts;
    }

    private EmergencyContactFact emergencyContact(Long id,
                                                  String code,
                                                  String title,
                                                  String description,
                                                  String phone,
                                                  String badgeType,
                                                  String badgeLabel,
                                                  String locale) {
        return new EmergencyContactFact(
            id,
            "emergency_contact:" + id,
            code,
            title,
            description,
            phone,
            phone,
            badgeType,
            badgeLabel,
            locale
        );
    }

    private GroundedAnswer generateValidatedLlmAnswer(String question,
                                                      IntentResult intent,
                                                      RagContext context,
                                                      GroundedAnswer fallback) {
        try {
            GroundedAnswer llmAnswer = llmAnswerService.generate(question, intent, context);
            if (llmAnswerValidator.isValid(llmAnswer, context)) {
                return llmAnswer;
            }
            log.warn("LLM answer validation failed. intent={}, sourceIds={}", intent.intent(), llmAnswer == null ? null : llmAnswer.sourceIds());
        } catch (RuntimeException ex) {
            log.warn("LLM generation failed. intent={}", intent.intent(), ex);
        }
        return fallback;
    }

    private ChatIntent inferResponseIntent(IntentResult originalIntent, GroundedAnswer answer, RagContext context) {
        if (answer == null || answer.sourceIds() == null || answer.sourceIds().isEmpty()) {
            return originalIntent.intent();
        }
        Set<SourceType> selectedTypes = context.sources().stream()
            .filter(source -> answer.sourceIds().contains(source.sourceId()))
            .map(RagContext.SourceDocument::sourceType)
            .collect(java.util.stream.Collectors.toSet());
        if (selectedTypes.contains(SourceType.EMERGENCY_CONTACT)) {
            return originalIntent.intent() == ChatIntent.PHONE_NUMBER ? ChatIntent.PHONE_NUMBER : ChatIntent.EMERGENCY_CONTACT;
        }
        if (selectedTypes.contains(SourceType.OPERATING_HOUR)) {
            return ChatIntent.OPERATING_HOURS;
        }
        if (selectedTypes.contains(SourceType.TOUR_PLACE)) {
            return ChatIntent.TOUR_PLACE_SEARCH;
        }
        return originalIntent.intent();
    }

    private AnswerType inferAnswerType(ChatIntent intent) {
        return switch (intent) {
            case OPERATING_HOURS -> AnswerType.OPERATING_HOURS;
            case PHONE_NUMBER -> AnswerType.PHONE_NUMBERS;
            case EMERGENCY_CONTACT -> AnswerType.EMERGENCY_CONTACTS;
            case TOUR_PLACE_SEARCH, GENERAL_TOURISM -> AnswerType.OPEN_DETAIL_PAGE;
            case UNKNOWN -> AnswerType.NO_DATA;
        };
    }

    private GroundedAnswer renderTemplateAnswer(AnswerType answerType, GroundedAnswer selectedAnswer, CandidateAnswers candidates) {
        if (selectedAnswer == null || selectedAnswer.sourceIds() == null || selectedAnswer.sourceIds().isEmpty()) {
            return selectedAnswer;
        }

        GroundedAnswer templated = switch (answerType) {
            case OPERATING_HOURS -> templateAnswerService.operatingHours(filterBySourceIds(
                candidates.hours(),
                selectedAnswer.sourceIds(),
                OperatingHourFact::sourceId
            ));
            case EMERGENCY_CONTACTS -> templateAnswerService.emergencyContacts(filterBySourceIds(
                candidates.contacts(),
                selectedAnswer.sourceIds(),
                EmergencyContactFact::sourceId
            ));
            case PHONE_NUMBERS -> templateAnswerService.phoneNumbers(filterBySourceIds(
                candidates.contacts(),
                selectedAnswer.sourceIds(),
                EmergencyContactFact::sourceId
            ));
            case OPEN_DETAIL_PAGE -> templateAnswerService.tourPlaces(filterBySourceIds(
                candidates.places(),
                selectedAnswer.sourceIds(),
                TourPlaceFact::sourceId
            ));
            case NO_DATA -> selectedAnswer;
        };

        if (templated == null || templated.answer() == null || templated.answer().isBlank()) {
            return selectedAnswer;
        }
        return new GroundedAnswer(
            templated.answer(),
            templated.sourceIds(),
            templated.grounded(),
            selectedAnswer.llmUsed()
        );
    }

    private <T> List<T> filterBySourceIds(List<T> values, List<String> sourceIds, Function<T, String> sourceIdAccessor) {
        Set<String> selected = Set.copyOf(sourceIds);
        return values.stream()
            .filter(value -> selected.contains(sourceIdAccessor.apply(value)))
            .collect(Collectors.toList());
    }

    private ChatResponse toResponse(ChatIntent intent, String locale, GroundedAnswer answer, RagContext context) {
        return new ChatResponse(
            answer.answer(),
            intent.name(),
            answer.grounded(),
            answer.llmUsed(),
            false,
            answer.sourceIds(),
            context.sources().stream()
                .filter(source -> answer.sourceIds().contains(source.sourceId()))
                .map(source -> new ChatResponse.Citation(source.sourceId(), source.sourceType().name(), source.title()))
                .toList()
        );
    }

    private record CandidateAnswers(
        List<TourPlaceFact> places,
        List<OperatingHourFact> hours,
        List<EmergencyContactFact> contacts,
        RagContext context,
        GroundedAnswer fallback
    ) {
    }
}
