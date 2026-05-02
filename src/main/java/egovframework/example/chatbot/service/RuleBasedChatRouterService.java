package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.domain.SearchPlan;
import egovframework.example.chatbot.dto.ChatRoute;
import egovframework.example.chatbot.dto.IntentResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(ChatRouterService.class)
public class RuleBasedChatRouterService implements ChatRouterService {

    private final IntentClassifier intentClassifier;

    public RuleBasedChatRouterService(IntentClassifier intentClassifier) {
        this.intentClassifier = intentClassifier;
    }

    @Override
    public ChatRoute route(String question, String normalizedMessage, String locale) {
        IntentResult intent = intentClassifier.classify(normalizedMessage, locale);
        return new ChatRoute(
            intent.intent(),
            searchPlan(intent),
            intent.keyword(),
            null,
            intent.confidence()
        );
    }

    private SearchPlan searchPlan(IntentResult intent) {
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
            case TOUR_PLACE_SEARCH -> SearchPlan.PLACE_RECOMMENDATION;
            case GENERAL_TOURISM -> SearchPlan.PLACE_RECOMMENDATION;
            case UNKNOWN -> SearchPlan.NONE;
        };
    }
}
