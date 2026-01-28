package egovframework.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import egovframework.example.domain.TourPlace;
import egovframework.example.domain.TourPlaceI18n;
import egovframework.example.repository.TourPlaceI18nRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourPlaceAutoTranslateService {

    private static final Logger log = LoggerFactory.getLogger(TourPlaceAutoTranslateService.class);
    private final TourPlaceI18nRepository i18nRepo;
    private final DeepLClient deepLClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate txTemplate;

    public TourPlaceAutoTranslateService(TourPlaceI18nRepository i18nRepo,
                                         DeepLClient deepLClient,
                                         TransactionTemplate txTemplate) {
        this.i18nRepo = i18nRepo;
        this.deepLClient = deepLClient;
        this.txTemplate = txTemplate;
        this.txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void ensureLocaleAndAttachDisplay(List<TourPlace> places, String requestedLocale) {
        if (places == null || places.isEmpty()) return;

        String locale = normalizeLocale(requestedLocale); // "en","ru","tg"
        log.info("TourPlace i18n ensure: locale={}, places={}", locale, places.size());

        // No need to create for EN requests, but still set displayI18n.
        List<Long> placeIds = places.stream().map(TourPlace::getPlaceId).collect(Collectors.toList());

        // 1) Load translations for the requested locale in one go
        Map<Long, TourPlaceI18n> targetMap = i18nRepo.findByPlace_PlaceIdInAndLocale(placeIds, locale)
                .stream().collect(Collectors.toMap(x -> x.getPlace().getPlaceId(), x -> x));

        // 2) Find missing placeIds
        List<Long> missingIds = new ArrayList<>();
        for (Long id : placeIds) {
            TourPlaceI18n existing = targetMap.get(id);
            boolean needsTranslation = existing == null
                    || isBlank(existing.getTitle())
                    || isBlank(existing.getContent())
                    || isBlank(existing.getAddress());
            if (needsTranslation) {
                missingIds.add(id);
                targetMap.remove(id); // prevent blank entries from being used as display
            }
        }
        log.info("TourPlace i18n missing: locale={}, missing={}", locale, missingIds.size());

        if (!missingIds.isEmpty() && !locale.equals("en")) {
            // 3) Load EN originals in one go
            Map<Long, TourPlaceI18n> enMap = i18nRepo.findByPlace_PlaceIdInAndLocale(missingIds, "en")
                    .stream().collect(Collectors.toMap(x -> x.getPlace().getPlaceId(), x -> x));

            // 4) Create and save translations for missing entries
            for (Long placeId : missingIds) {
                TourPlaceI18n en = enMap.get(placeId);
                if (en == null) continue; // Skip if no EN source (or throw)

                TourPlaceI18n existing = i18nRepo.findByPlace_PlaceIdAndLocale(placeId, locale).orElse(null);

                String targetLang = toDeepLTarget(locale); // "RU" or "TG"
                boolean enableBeta = locale.equals("tg");  // Treat TG as beta

                String title = extractTranslatedText(
                        deepLClient.translate(en.getTitle(), "EN", targetLang, enableBeta)
                );
                String content = extractTranslatedText(
                        deepLClient.translate(en.getContent(), "EN", targetLang, enableBeta)
                );
                String address = extractTranslatedText(
                        deepLClient.translate(en.getAddress(), "EN", targetLang, enableBeta)
                );
                if (title == null && content == null && address == null) {
                    log.warn("DeepL returned empty translations: placeId={}, locale={}", placeId, locale);
                }

                TourPlaceI18n upsert = existing != null ? existing : new TourPlaceI18n();
                if (existing == null) {
                    upsert.setPlace(en.getPlace());
                    upsert.setLocale(locale);
                }
                upsert.setTitle(!isBlank(title) ? title
                        : (!isBlank(upsert.getTitle()) ? upsert.getTitle() : en.getTitle()));
                upsert.setContent(!isBlank(content) ? content
                        : (!isBlank(upsert.getContent()) ? upsert.getContent() : en.getContent()));
                upsert.setAddress(!isBlank(address) ? address
                        : (!isBlank(upsert.getAddress()) ? upsert.getAddress() : en.getAddress()));

                // UNIQUE(place_id, locale) can conflict on concurrent requests.
                // A minimal implementation can handle it with try-catch.
                try {
                    TourPlaceI18n saved = txTemplate.execute(status -> i18nRepo.save(upsert));
                    if (saved != null) {
                        targetMap.put(placeId, saved);
                    }
                } catch (Exception ex) {
                    log.error("TourPlace i18n save failed: placeId={}, locale={}, err={}", placeId, locale, ex.getMessage());
                    // Another request may have saved first; re-fetch and use
                    i18nRepo.findByPlace_PlaceIdAndLocale(placeId, locale)
                            .ifPresent(v -> targetMap.put(placeId, v));
                }
            }
        }

        // 5) Set displayI18n (fallback to EN if missing)
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
        if (locale.equals("ru") || locale.equals("tg") || locale.equals("en") || locale.equals("ko")) return locale;
        return "en";
    }

    private String toDeepLTarget(String locale) {
        switch (locale) {
            case "ru": return "RU";
            case "tg": return "TG";
            case "ko": return "KO";
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
