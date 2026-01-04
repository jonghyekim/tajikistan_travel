package egovframework.example.service;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import egovframework.example.domain.CategoryCode;
import egovframework.example.domain.CategoryCodeI18n;
import egovframework.example.domain.RegionCode;
import egovframework.example.domain.RegionCodeI18n;
import egovframework.example.repository.CategoryCodeI18nRepository;
import egovframework.example.repository.RegionCodeI18nRepository;

@Service
public class CodeAutoTranslateService {

    private final CategoryCodeI18nRepository catI18nRepo;
    private final RegionCodeI18nRepository regI18nRepo;
    private final DeepLClient deepLClient;

    public CodeAutoTranslateService(CategoryCodeI18nRepository catI18nRepo,
                                   RegionCodeI18nRepository regI18nRepo,
                                   DeepLClient deepLClient) {
        this.catI18nRepo = catI18nRepo;
        this.regI18nRepo = regI18nRepo;
        this.deepLClient = deepLClient;
    }

    @Transactional
    public void ensureAndAttachCategoryNames(List<CategoryCode> categories, String requestedLocale) {
        if (categories == null || categories.isEmpty()) return;
        String locale = normalize(requestedLocale);

        // EN이면 그냥 EN을 붙이거나, 그냥 code를 써도 되지만 name을 보여주려면 EN i18n을 붙이는게 좋음
        List<String> codes = categories.stream().map(CategoryCode::getCode).collect(Collectors.toList());

        Map<String, String> targetMap = catI18nRepo.findByCategoryCode_CodeInAndLocale(codes, locale)
                .stream().collect(Collectors.toMap(x -> x.getCategoryCode().getCode(), CategoryCodeI18n::getName));

        List<String> missing = codes.stream().filter(c -> !targetMap.containsKey(c)).collect(Collectors.toList());

        if (!missing.isEmpty() && !locale.equals("en")) {
            Map<String, String> enMap = catI18nRepo.findByCategoryCode_CodeInAndLocale(missing, "en")
                    .stream().collect(Collectors.toMap(x -> x.getCategoryCode().getCode(), CategoryCodeI18n::getName));

            for (String code : missing) {
                String enName = enMap.get(code);
                if (enName == null) continue;

                String targetLang = toDeepL(locale); // RU/TG
                boolean enableBeta = locale.equals("tg");

                String translated = deepLClient.translateTextOnly(enName, "EN", targetLang, enableBeta);

                CategoryCodeI18n created = new CategoryCodeI18n();
                CategoryCode cc = categories.stream().filter(x -> x.getCode().equals(code)).findFirst().orElse(null);
                if (cc == null) continue;
                created.setCategoryCode(cc);
                created.setLocale(locale);
                created.setName(translated != null ? translated : enName);

                try {
                    catI18nRepo.save(created);
                    targetMap.put(code, created.getName());
                } catch (Exception e) {
                    catI18nRepo.findByCategoryCode_CodeAndLocale(code, locale)
                            .ifPresent(v -> targetMap.put(code, v.getName()));
                }
            }
        }

        // fallback EN
        Map<String, String> enFallback = catI18nRepo.findByCategoryCode_CodeInAndLocale(codes, "en")
                .stream().collect(Collectors.toMap(x -> x.getCategoryCode().getCode(), CategoryCodeI18n::getName, (a,b)->a));

        for (CategoryCode c : categories) {
            String name = targetMap.getOrDefault(c.getCode(), enFallback.getOrDefault(c.getCode(), c.getCode()));
            c.setDisplayName(name);
        }
    }
    
    @Transactional
    public void attachDisplayNamesForPlaces(List<egovframework.example.domain.TourPlace> places, String requestedLocale) {
        if (places == null || places.isEmpty()) return;
        String locale = normalize(requestedLocale);

        // places에서 code 목록 뽑기
        List<String> categoryCodes = places.stream()
                .map(p -> p.getCategory().getCode())
                .distinct()
                .collect(Collectors.toList());

        List<String> regionCodes = places.stream()
                .map(p -> p.getRegion().getCode())
                .distinct()
                .collect(Collectors.toList());

        // 드롭다운용 메서드 재사용하려면 엔티티 리스트가 필요하니
        // place에 붙은 category/region 엔티티들을 모아서 그대로 넘기면 됨
        List<CategoryCode> categories = places.stream()
                .map(egovframework.example.domain.TourPlace::getCategory)
                .distinct()
                .collect(Collectors.toList());

        List<RegionCode> regions = places.stream()
                .map(egovframework.example.domain.TourPlace::getRegion)
                .distinct()
                .collect(Collectors.toList());

        // 이 호출이 region/category displayName을 세팅해줌
        ensureAndAttachCategoryNames(categories, locale);
        ensureAndAttachRegionNames(regions, locale);

        // 혹시라도 동일 코드가 여러 객체로 들어온 경우 대비해서,
        // code->displayName 맵을 다시 만들어 place에 확실히 세팅
        Map<String, String> catNameMap = categories.stream()
                .collect(Collectors.toMap(CategoryCode::getCode, CategoryCode::getDisplayName, (a,b)->a));

        Map<String, String> regNameMap = regions.stream()
                .collect(Collectors.toMap(RegionCode::getCode, RegionCode::getDisplayName, (a,b)->a));

        for (var p : places) {
            p.getCategory().setDisplayName(catNameMap.get(p.getCategory().getCode()));
            p.getRegion().setDisplayName(regNameMap.get(p.getRegion().getCode()));
        }
    }

    @Transactional
    public void ensureAndAttachRegionNames(List<RegionCode> regions, String requestedLocale) {
        if (regions == null || regions.isEmpty()) return;
        String locale = normalize(requestedLocale);

        List<String> codes = regions.stream().map(RegionCode::getCode).collect(Collectors.toList());

        Map<String, String> targetMap = regI18nRepo.findByRegionCode_CodeInAndLocale(codes, locale)
                .stream().collect(Collectors.toMap(x -> x.getRegionCode().getCode(), RegionCodeI18n::getName));

        List<String> missing = codes.stream().filter(c -> !targetMap.containsKey(c)).collect(Collectors.toList());

        if (!missing.isEmpty() && !locale.equals("en")) {
            Map<String, String> enMap = regI18nRepo.findByRegionCode_CodeInAndLocale(missing, "en")
                    .stream().collect(Collectors.toMap(x -> x.getRegionCode().getCode(), RegionCodeI18n::getName));

            for (String code : missing) {
                String enName = enMap.get(code);
                if (enName == null) continue;

                String targetLang = toDeepL(locale);
                boolean enableBeta = locale.equals("tg");

                String translated = deepLClient.translateTextOnly(enName, "EN", targetLang, enableBeta);

                RegionCodeI18n created = new RegionCodeI18n();
                RegionCode rc = regions.stream().filter(x -> x.getCode().equals(code)).findFirst().orElse(null);
                if (rc == null) continue;
                created.setRegionCode(rc);
                created.setLocale(locale);
                created.setName(translated != null ? translated : enName);

                try {
                    regI18nRepo.save(created);
                    targetMap.put(code, created.getName());
                } catch (Exception e) {
                    regI18nRepo.findByRegionCode_CodeAndLocale(code, locale)
                            .ifPresent(v -> targetMap.put(code, v.getName()));
                }
            }
        }

        Map<String, String> enFallback = regI18nRepo.findByRegionCode_CodeInAndLocale(codes, "en")
                .stream().collect(Collectors.toMap(x -> x.getRegionCode().getCode(), RegionCodeI18n::getName, (a,b)->a));

        for (RegionCode r : regions) {
            String name = targetMap.getOrDefault(r.getCode(), enFallback.getOrDefault(r.getCode(), r.getCode()));
            r.setDisplayName(name);
        }
    }

    private String normalize(String locale) {
        if (locale == null || locale.isBlank()) return "en";
        locale = locale.trim().toLowerCase();
        return (locale.equals("ru") || locale.equals("tg") || locale.equals("en")) ? locale : "en";
    }

    private String toDeepL(String locale) {
        return locale.toUpperCase(); // RU/TG
    }
}
