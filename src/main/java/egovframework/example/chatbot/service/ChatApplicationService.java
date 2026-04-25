package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.dto.ChatRequest;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);

    private final MessageNormalizer messageNormalizer;
    private final IntentClassifier intentClassifier;
    private final TourPlaceQueryService tourPlaceQueryService;
    private final EmergencyContactQueryService emergencyContactQueryService;
    private final RagContextBuilder ragContextBuilder;
    private final TemplateAnswerService templateAnswerService;
    private final LlmAnswerService llmAnswerService;
    private final LlmAnswerValidator llmAnswerValidator;

    public ChatApplicationService(MessageNormalizer messageNormalizer,
                                  IntentClassifier intentClassifier,
                                  TourPlaceQueryService tourPlaceQueryService,
                                  EmergencyContactQueryService emergencyContactQueryService,
                                  RagContextBuilder ragContextBuilder,
                                  TemplateAnswerService templateAnswerService,
                                  LlmAnswerService llmAnswerService,
                                  LlmAnswerValidator llmAnswerValidator) {
        this.messageNormalizer = messageNormalizer;
        this.intentClassifier = intentClassifier;
        this.tourPlaceQueryService = tourPlaceQueryService;
        this.emergencyContactQueryService = emergencyContactQueryService;
        this.ragContextBuilder = ragContextBuilder;
        this.templateAnswerService = templateAnswerService;
        this.llmAnswerService = llmAnswerService;
        this.llmAnswerValidator = llmAnswerValidator;
    }

    @Cacheable(cacheNames = "chatbot:responses", key = "#request.message() + ':' + #request.locale()", unless = "#result.noData()")
    public ChatResponse chat(ChatRequest request) {
        String normalizedMessage = messageNormalizer.normalize(request.message());
        String locale = messageNormalizer.normalizeLocale(request.locale());
        IntentResult intent = intentClassifier.classify(normalizedMessage, locale);

        return switch (intent.intent()) {
            case OPERATING_HOURS -> answerOperatingHours(intent);
            case EMERGENCY_CONTACT, PHONE_NUMBER -> answerEmergencyContacts(intent);
            case TOUR_PLACE_SEARCH -> answerTourPlaces(request.message(), intent);
            case GENERAL_TOURISM, UNKNOWN -> ChatResponse.noData(intent.intent().name(), intent.locale());
        };
    }

    private ChatResponse answerOperatingHours(IntentResult intent) {
        if (intent.keyword() == null || intent.keyword().isBlank()) {
            return ChatResponse.noData(intent.intent().name(), intent.locale());
        }
        List<OperatingHourFact> hours = tourPlaceQueryService.findOperatingHours(intent.keyword(), intent.locale());
        if (hours.isEmpty()) {
            return ChatResponse.noData(intent.intent().name(), intent.locale());
        }
        GroundedAnswer answer = templateAnswerService.operatingHours(hours);
        RagContext context = ragContextBuilder.fromOperatingHours(hours);
        return toResponse(intent, answer, context);
    }

    private ChatResponse answerEmergencyContacts(IntentResult intent) {
        List<EmergencyContactFact> contacts;
        try {
            contacts = emergencyContactQueryService.findActiveContacts(intent.keyword(), intent.locale());
        } catch (RuntimeException ex) {
            log.warn("Emergency contact lookup failed. keyword={}, locale={}", intent.keyword(), intent.locale(), ex);
            contacts = fallbackEmergencyContacts(intent);
        }
        if (contacts.isEmpty()) {
            return ChatResponse.noData(intent.intent().name(), intent.locale());
        }
        GroundedAnswer answer = intent.intent() == ChatIntent.PHONE_NUMBER
            ? templateAnswerService.phoneNumbers(contacts)
            : templateAnswerService.emergencyContacts(contacts);
        RagContext context = ragContextBuilder.fromEmergencyContacts(contacts);
        return toResponse(intent, answer, context);
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

    private ChatResponse answerTourPlaces(String question, IntentResult intent) {
        if (intent.keyword() == null || intent.keyword().isBlank()) {
            return ChatResponse.noData(intent.intent().name(), intent.locale());
        }
        List<TourPlaceFact> places = tourPlaceQueryService.searchPlaces(intent.keyword(), intent.locale());
        if (places.isEmpty()) {
            return ChatResponse.noData(intent.intent().name(), intent.locale());
        }

        RagContext context = ragContextBuilder.fromTourPlaces(places);
        GroundedAnswer fallback = templateAnswerService.tourPlaces(places);
        GroundedAnswer answer = generateValidatedLlmAnswer(question, intent, context, fallback);
        return toResponse(intent, answer, context);
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

    private ChatResponse toResponse(IntentResult intent, GroundedAnswer answer, RagContext context) {
        return new ChatResponse(
            answer.answer(),
            intent.intent().name(),
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
}
