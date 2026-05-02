package egovframework.example.chatbot.support;

import java.util.Locale;

public final class ChatbotLexicon {

    private ChatbotLexicon() {
    }

    public static String normalizeQuery(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message.toLowerCase(Locale.ROOT)
            .replace("루다키공원", "루다키 공원")
            .replace("미국대사관", "미국 대사관")
            .replace("경찰번호좀", "경찰 번호")
            .replace("번호좀", "번호")
            .replace("두산베", "두샨베")
            .replace("dushnabe", "dushanbe")
            .replace("dushambe", "dushanbe")
            .replace("prk", "park")
            .replace("ambulnce", "ambulance")
            .replace("embasy", "embassy")
            .replace("душанбэ", "душанбе")
            .replace("душанде", "душанбе")
            .replace("рестаран", "ресторан")
            .replace("отел", "отель")
            .replace("отельь", "отель")
            .replace("гостинца", "гостиница")
            .replace("полицйи", "полиции")
            .replace("тарабхонахо", "тарабхонаҳо")
            .replace("мехмонхона", "меҳмонхона")
            .replace("хурок", "хӯрок")
            .replace("таъчили", "таъҷилӣ")
            .replace("ракам", "рақам")
            .replace("сухтор", "сӯхтор")
            .replace("боги", "боғи")
            .replace("бог", "боғ");
    }

    public static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        String normalized = normalizeQuery(token.trim())
            .replaceAll("^[\\p{Punct}؟،。！？]+|[\\p{Punct}؟،。！？]+$", "");
        normalized = normalized.replaceAll("(에서|으로|로|의|에)$", "");
        return normalized;
    }

    public static String categoryCode(String token) {
        String normalized = normalizeToken(token);
        if (normalized.startsWith("숙박") || normalized.startsWith("호텔")) {
            return "STAY";
        }
        if (normalized.startsWith("останов")) {
            return "STAY";
        }
        if (normalized.startsWith("식사") || normalized.startsWith("식당")
            || normalized.startsWith("음식") || normalized.startsWith("맛집")
            || normalized.startsWith("밥집") || normalized.startsWith("먹을")) {
            return "DINING";
        }
        if (normalized.startsWith("마싯") || normalized.startsWith("맛잇") || normalized.startsWith("맛있")) {
            return "DINING";
        }
        if (normalized.startsWith("공원") || normalized.startsWith("공언")) {
            return "PARK";
        }
        return switch (normalized) {
            case "dining", "restaurant", "restaurants", "restarant", "restraunt", "food", "cafe", "cafes",
                 "식당", "음식점", "레스토랑", "맛집", "밥집",
                 "ресторан", "рестораны", "ресторанҳо", "кафе", "поесть", "тарабхона", "тарабхонаҳо", "хӯрок" -> "DINING";
            case "stay", "hotel", "hotels", "accommodation", "숙소", "호텔",
                 "отель", "гостиница", "гостиницы", "меҳмонхона", "меҳмонхонаҳо" -> "STAY";
            case "park", "parks", "zoo", "공원", "공언", "동물원", "парк", "парки", "боғ", "боғи" -> "PARK";
            case "destination", "destinations", "attraction", "attractions", "관광지", "명소" -> "DESTINATION";
            default -> null;
        };
    }

    public static String regionCode(String token) {
        String normalized = normalizeToken(token);
        return switch (normalized) {
            case "dushanbe", "душанбе", "두샨베" -> "DUSHANBE";
            case "khujand", "sughd", "쿠잔드", "수그드" -> "SUGHD";
            case "khatlon", "하틀론" -> "KHATLON";
            case "gbao", "pamir", "khorog", "파미르", "호로그" -> "GBAO";
            case "hisor", "rogun", "drs", "히소르", "로군" -> "DRS";
            default -> null;
        };
    }

    public static boolean isSearchStopword(String token) {
        String normalized = normalizeToken(token);
        if (normalized.matches("\\d+.*")) {
            return true;
        }
        return switch (normalized) {
            case "좋은", "괜찮은", "가볼만한", "갈만한", "추천", "추천해줘", "알려줘", "보여줘",
                 "위주로", "가볍게", "둘러볼", "둘러보기", "만한", "곳", "뭐", "있음", "있어", "있나요",
                 "조용한", "산책", "산책하기", "근처", "어디가", "어디", "위치", "주소", "뭐야", "야", "아이랑", "ㅊㅊ", "ᄎᄎ", "pls", "plz",
                 "best", "good", "top", "nice", "recommended", "recommendation", "quiet", "walking", "walk", "stroll",
                 "can", "i", "any", "does", "hrs", "where", "address", "location", "located", "рекоменд", "посоветуйте", "хороший", "тихий", "прогулка",
                 "где", "во", "сколько", "в", "на", "дар", "аст", "ҳаст", "тавсия", "диҳед", "чанд" -> true;
            default -> false;
        };
    }

    public static boolean isGenericPlaceToken(String token) {
        String normalized = normalizeToken(token);
        return categoryCode(normalized) != null || switch (normalized) {
            case "парк", "парки", "боғ", "боғи", "park", "parks", "공원" -> true;
            default -> false;
        };
    }

    public static boolean isEmergencyStopword(String token) {
        String normalized = normalizeToken(token);
        if (normalized.matches("\\d+.*")) {
            return false;
        }
        return switch (normalized) {
            case "타지키스탄", "타지키스탄에서", "뭐", "뭐야", "야", "번호", "번호좀",
                 "긴급", "긴급상황", "상황", "연락처", "필요", "필요해", "필요합니다",
                 "tajikistan", "in", "the", "a", "an", "number", "numbr", "contact", "emergency", "need",
                 "в", "на", "и", "дар", "тоҷикистон", "таджикистане",
                 "телефон", "номер", "рақами", "рақам", "тамос" -> true;
            default -> false;
        };
    }

    public static String contactAlias(String token) {
        String normalized = normalizeToken(token);
        return switch (normalized) {
            case "경찰", "police", "полиция", "полиции", "пулис" -> "police";
            case "구급차", "응급", "ambulance", "скорая", "ёрии", "таъҷилӣ" -> "ambulance";
            case "소방", "fire", "пожарная", "сӯхтор" -> "fire";
            case "대사관", "embassy", "посольство", "сафорат" -> "embassy";
            default -> normalized;
        };
    }
}
