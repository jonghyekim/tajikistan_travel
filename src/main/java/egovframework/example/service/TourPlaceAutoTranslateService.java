package egovframework.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import egovframework.example.domain.TourPlace;
import egovframework.example.domain.TourPlaceI18n;
import egovframework.example.repository.TourPlaceI18nRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourPlaceAutoTranslateService {

    private final TourPlaceI18nRepository i18nRepo;
    private final DeepLClient deepLClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TourPlaceAutoTranslateService(TourPlaceI18nRepository i18nRepo, DeepLClient deepLClient) {
        this.i18nRepo = i18nRepo;
        this.deepLClient = deepLClient;
    }

    @Transactional
    public void ensureLocaleAndAttachDisplay(List<TourPlace> places, String requestedLocale) {
        if (places == null || places.isEmpty()) return;

        String locale = normalizeLocale(requestedLocale); // "en","ru","tg"

        // en 요청이면 굳이 생성할 필요 없음. 그래도 displayI18n은 세팅하자.
        List<Long> placeIds = places.stream().map(TourPlace::getPlaceId).collect(Collectors.toList());

        // 1) 요청 locale 번역들 한번에 로딩
        Map<Long, TourPlaceI18n> targetMap = i18nRepo.findByPlace_PlaceIdInAndLocale(placeIds, locale)
                .stream().collect(Collectors.toMap(x -> x.getPlace().getPlaceId(), x -> x));

        // 2) 없는 placeId 찾기
        List<Long> missingIds = placeIds.stream()
                .filter(id -> !targetMap.containsKey(id))
                .collect(Collectors.toList());

        if (!missingIds.isEmpty() && !locale.equals("en")) {
            // 3) EN 원문들을 한번에 로딩
            Map<Long, TourPlaceI18n> enMap = i18nRepo.findByPlace_PlaceIdInAndLocale(missingIds, "en")
                    .stream().collect(Collectors.toMap(x -> x.getPlace().getPlaceId(), x -> x));

            // 4) missing에 대해 번역 생성 + 저장
            for (Long placeId : missingIds) {
                TourPlaceI18n en = enMap.get(placeId);
                if (en == null) continue; // EN 원문 없으면 스킵(또는 예외)

                String targetLang = toDeepLTarget(locale); // "RU" or "TG"
                boolean enableBeta = locale.equals("tg");  // TG는 베타 취급

                String title = extractTranslatedText(
                        deepLClient.translate(en.getTitle(), "EN", targetLang, enableBeta)
                );
                String content = extractTranslatedText(
                        deepLClient.translate(en.getContent(), "EN", targetLang, enableBeta)
                );
                String address = extractTranslatedText(
                        deepLClient.translate(en.getAddress(), "EN", targetLang, enableBeta)
                );

                TourPlaceI18n created = new TourPlaceI18n();
                created.setPlace(en.getPlace());
                created.setLocale(locale);
                created.setTitle(title != null ? title : en.getTitle());
                created.setContent(content);
                created.setAddress(address);

                // UNIQUE(place_id, locale) 때문에 동시에 들어오면 충돌날 수 있음.
                // 최소 구현은 try-catch로 처리해도 됨.
                try {
                    TourPlaceI18n saved = i18nRepo.save(created);
                    targetMap.put(placeId, saved);
                } catch (Exception ex) {
                    // 다른 요청이 먼저 저장했을 수도 있으니 다시 조회해서 사용
                    i18nRepo.findByPlace_PlaceIdAndLocale(placeId, locale)
                            .ifPresent(v -> targetMap.put(placeId, v));
                }
            }
        }

        // 5) displayI18n 세팅 (없으면 EN fallback)
        Map<Long, TourPlaceI18n> enFallbackMap = i18nRepo.findByPlace_PlaceIdInAndLocale(placeIds, "en")
                .stream().collect(Collectors.toMap(x -> x.getPlace().getPlaceId(), x -> x));

        for (TourPlace p : places) {
            TourPlaceI18n chosen = targetMap.getOrDefault(p.getPlaceId(), enFallbackMap.get(p.getPlaceId()));
            p.setDisplayI18n(chosen);
        }
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) return "en";
        locale = locale.trim().toLowerCase();
        if (locale.equals("ru") || locale.equals("tg") || locale.equals("en")) return locale;
        return "en";
    }

    private String toDeepLTarget(String locale) {
        switch (locale) {
            case "ru": return "RU";
            case "tg": return "TG";
            default: return "EN";
        }
    }

    private String extractTranslatedText(String deeplRawJson) {
        if (deeplRawJson == null || deeplRawJson.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(deeplRawJson);
            JsonNode translations = root.get("translations");
            if (translations != null && translations.isArray() && translations.size() > 0) {
                JsonNode text = translations.get(0).get("text");
                return text != null ? text.asText() : null;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
