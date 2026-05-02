package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.dto.IntentResult;
import egovframework.example.chatbot.support.ChatbotLexicon;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IntentClassifier {

    public IntentResult classify(String normalizedMessage, String locale) {
        String normalizedQuery = ChatbotLexicon.normalizeQuery(normalizedMessage);
        ChatIntent intent = resolveIntent(normalizedQuery);
        String keyword = intent == ChatIntent.EMERGENCY_CONTACT || intent == ChatIntent.PHONE_NUMBER
            ? extractEmergencyKeyword(normalizedQuery)
            : extractKeyword(normalizedQuery);
        return new IntentResult(
            intent,
            intent == ChatIntent.UNKNOWN ? 0.2 : 0.85,
            normalizedMessage,
            keyword,
            locale,
            Map.of(),
            isExactInformationIntent(intent)
        );
    }

    private ChatIntent resolveIntent(String message) {
        if (isBlankOrNoise(message) || isInternalInformationRequest(message)) {
            return ChatIntent.UNKNOWN;
        }
        if (containsAny(message, "emergency", "police", "ambulance", "hospital", "긴급", "응급", "경찰", "대사관", "전화", "contact",
            "экстр", "полиция", "скорая", "больница", "посольство", "тамос", "изтирор", "пулис", "ёрии таъҷилӣ", "сафорат")) {
            return ChatIntent.EMERGENCY_CONTACT;
        }
        if (containsAny(message, "open", "close", "hour", "hours", "time", "hrs", "운영", "영업", "시간", "몇시에", "열어",
            "откры", "закры", "час", "время", "часы", "когда", "соат", "кушод", "баста")) {
            return ChatIntent.OPERATING_HOURS;
        }
        if (containsAny(message, "phone", "number", "numbr", "전화번호", "번호", "연락처", "телефон", "номер", "рақам")
            || containsWord(message, "tel")) {
            return ChatIntent.PHONE_NUMBER;
        }
        if (containsAny(message, "place", "tour", "visit", "attraction", "park", "parks", "zoo", "hotel", "restaurant", "restarant", "restraunt", "food", "cafe", "where", "address", "location",
            "관광", "여행", "명소", "추천", "공원", "공언", "동물원", "호텔", "숙소", "식당", "맛집", "밥집", "가볼만한", "갈만한", "둘러볼", "곳", "위치", "주소", "어디",
            "мест", "тур", "достопримеч", "посет", "парк", "отель", "гостиниц", "ресторан", "останов", "кафе", "поесть",
            "саёҳ", "ҷой", "тамошобоб", "боғ", "меҳмонхона", "тарабхона", "хӯрок")) {
            return ChatIntent.TOUR_PLACE_SEARCH;
        }
        return ChatIntent.GENERAL_TOURISM;
    }

    private boolean isBlankOrNoise(String message) {
        return message == null || message.isBlank() || !message.matches(".*[\\p{L}\\p{N}].*");
    }

    private boolean isInternalInformationRequest(String message) {
        return containsAny(message,
            "system prompt", "developer message", "internal prompt", "show sql", "database query", "db query",
            "시스템 프롬프트", "프롬프트 알려", "db 쿼리", "디비 쿼리", "sql 보여", "쿼리 보여", "내부 지시",
            "системный промпт", "промпт", "покажи запрос", "покажи sql");
    }

    private boolean containsAny(String message, String... tokens) {
        for (String token : tokens) {
            if (message.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWord(String message, String token) {
        return message != null && message.matches(".*(^|\\s)" + java.util.regex.Pattern.quote(token) + "(?=\\s|$).*");
    }

    private boolean isExactInformationIntent(ChatIntent intent) {
        return intent == ChatIntent.OPERATING_HOURS
            || intent == ChatIntent.PHONE_NUMBER
            || intent == ChatIntent.EMERGENCY_CONTACT;
    }

    private String extractKeyword(String message) {
        String keyword = message
            .replaceAll("\\b(what|where|when|which|who|how|are|is|the|a|an|to|me|please|tell|show|find|give|list|about|for|of|in|near|nearby|can|any|does)\\b", " ")
            .replaceAll("\\b(open|opening|close|closing|closed|hour|hours|time|phone|number|tel|contact|emergency|police|ambulance|fire|query|sql|prompt)\\b", " ")
            .replaceAll("\\b(recommend|recommendation|recommended|tour|tourist|travel|place|places|visit|attraction|attractions|destination|destinations|address|location|located|good|nice|best|top|quiet|walking|walk|stroll|pls|plz)\\b", " ")
            .replaceAll("(알려줘|보여줘|찾아줘|추천해줘|추천|운영|영업|시간|전화번호|전화|연락처|긴급|응급|경찰|구급차|소방|관광|여행|명소|근처|어디가|뭐야|야)", " ")
            .replaceAll("(위치가|위치는|위치|주소가|주소는|주소|어디야|어딨어|어디있어|어디인가요|어디|위주로|가볍게|둘러볼|둘러보기|만한|가볼만한|갈만한|좋은|괜찮은|조용한|산책하기|산책|뭐|있음|있어|있나요|곳)", " ")
            .replaceAll("(^|\\s)(где|во|сколько|в|на|дар|аст|ҳаст|тавсия|диҳед)(?=\\s|$)", " ")
            .replaceAll("(открыт|открыта|открыто|открытие|закрыт|закрыта|закрыто|часы|час|время|телефон|номер|контакт|экстренный|экстренная|туристические|туризм|места|место|достопримечательности|достопримечательность)", " ")
            .replaceAll("(рекоменд|посоветуйте|хорош|тих|прогул|кушода|баста|соат|вақт|телефон|рақам|тамос|изтирорӣ|изтирор|саёҳӣ|саёҳат|ҷой|ҷойҳо|тамошобоб)", " ")
            .replaceAll("[\\p{Punct}؟،。！？]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return keyword.isBlank() ? null : keyword;
    }

    private String extractEmergencyKeyword(String message) {
        String keyword = message
            .replaceAll("\\b(what|where|when|which|who|how|are|is|the|a|an|to|me|please|tell|show|find|give|list|about|for|of|in|near|nearby|can|any|does)\\b", " ")
            .replaceAll("\\b(phone|number|numbr|tel|contact|emergency)\\b", " ")
            .replaceAll("(알려줘|보여줘|찾아줘|전화번호|전화|연락처|번호|번호좀|긴급상황|긴급|상황|필요|필요해|필요합니다|좀|뭐야|야)", " ")
            .replaceAll("(^|\\s)(в|на|дар|аст|ҳаст|и)(?=\\s|$)", " ")
            .replaceAll("(телефон|номер|контакт|экстренный|экстренная|рақами|рақам|тамос|изтирорӣ|изтирор)", " ")
            .replaceAll("[\\p{Punct}؟،。！？]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return keyword.isBlank() ? null : keyword;
    }
}
