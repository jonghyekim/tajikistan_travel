package egovframework.example.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import egovframework.example.domain.CategoryCode;
import egovframework.example.domain.RegionCode;
import egovframework.example.domain.TourPlace;
import egovframework.example.repository.CategoryCodeRepository;
import egovframework.example.repository.RegionCodeRepository;
import egovframework.example.repository.TourPlaceRepository;
import egovframework.example.service.CodeAutoTranslateService;
import egovframework.example.service.TourPlaceAutoTranslateService;

// for image loading
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class HomeController {

    @Autowired private TourPlaceRepository tourPlaceRepository;
    @Autowired private CategoryCodeRepository categoryCodeRepository;
    @Autowired private RegionCodeRepository regionCodeRepository;
    @Autowired private TourPlaceAutoTranslateService tourPlaceAutoTranslateService;
    @Autowired private CodeAutoTranslateService codeAutoTranslateService;
    @Autowired private LocaleResolver localeResolver;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String lang,
                       Model model,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        String resolvedLang = resolveLang(lang, request, response);
        model.addAttribute("lang", resolvedLang);
        model.addAttribute("currentPage", "home");
        return "index"; // Home is a simple page with only a search box
    }
    
    @GetMapping("/filter")
    @Transactional
    public String filter(Model model, 
                         @RequestParam(required = false) String query,
                         @RequestParam(required = false) String category,
                         @RequestParam(required = false) String region,
                         @RequestParam(defaultValue = "1") Integer page,
                         @RequestParam(required = false) String lang,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String resolvedLang = resolveLang(lang, request, response);
        
    	List<CategoryCode> categories = categoryCodeRepository.findAll();
    	List<RegionCode> regions = regionCodeRepository.findAll();

    	codeAutoTranslateService.ensureAndAttachCategoryNames(categories, resolvedLang);
    	codeAutoTranslateService.ensureAndAttachRegionNames(regions, resolvedLang);
    	
        // Load lists to populate dropdowns
        model.addAttribute("lang", resolvedLang);
        model.addAttribute("categories", categories);
    	model.addAttribute("regions", regions);

        // Run search if any query exists
        // Searching runs here even when only query comes from home
    	// Removed the if so the default empty state shows all data (with the if, default state shows none)
//        if ((query != null && !query.trim().isEmpty()) || 
//            (category != null && !category.isEmpty()) || 
//            (region != null && !region.isEmpty())) {
    	
    		// Normalize null/empty/blank to keep option selection accurate
    		query = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
    		category = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
    		region = (region != null && !region.trim().isEmpty()) ? region.trim() : null;

            
            int pageSize = 9;
            int currentPage = (page == null || page < 1) ? 1 : page;
            Pageable pageable = PageRequest.of(
                    currentPage - 1,
                    pageSize,
                    Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "placeId"))
            );

            Page<TourPlace> resultPage = tourPlaceRepository.searchWithFilters(query, category, region, pageable);
            if (resultPage.getTotalPages() > 0 && currentPage > resultPage.getTotalPages()) {
                currentPage = resultPage.getTotalPages();
                pageable = PageRequest.of(
                        currentPage - 1,
                        pageSize,
                        Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "placeId"))
                );
                resultPage = tourPlaceRepository.searchWithFilters(query, category, region, pageable);
            }

            List<TourPlace> results = resultPage.getContent();
            
            // for image loading
            for (TourPlace p : results) {
                if (p.getImages() != null) {
                    p.getImages().size();
                }
                if (p.getCategory() != null) p.getCategory().getCode();
                if (p.getRegion() != null) p.getRegion().getCode();
            }
            
            tourPlaceAutoTranslateService.ensureLocaleAndAttachDisplay(results, resolvedLang);
            
            codeAutoTranslateService.attachDisplayNamesForPlaces(results, resolvedLang);
            
            model.addAttribute("results", results);
            model.addAttribute("totalResults", resultPage.getTotalElements());
            model.addAttribute("totalPages", resultPage.getTotalPages());
            model.addAttribute("currentPageNo", currentPage);

            int pageBlockSize = 5;
            int startPage = ((currentPage - 1) / pageBlockSize) * pageBlockSize + 1;
            int endPage = Math.min(startPage + pageBlockSize - 1, resultPage.getTotalPages());
            if (resultPage.getTotalPages() == 0) {
                startPage = 0;
                endPage = 0;
            }
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
            int prevBlockPage = Math.max(1, startPage - pageBlockSize);
            int nextBlockPage = Math.min(resultPage.getTotalPages(), startPage + pageBlockSize);
            model.addAttribute("prevBlockPage", prevBlockPage);
            model.addAttribute("nextBlockPage", nextBlockPage);
            
            model.addAttribute("lastQuery", query);
            model.addAttribute("lastCategory", category);
            model.addAttribute("lastRegion", region);
            model.addAttribute("lastPage", currentPage);
            
            // for navigation in header.html 
            model.addAttribute("currentPage", "filter");

//        }


        return "filter"; // Navigate to src/main/resources/templates/filter.html
    }
    
    // detail page added
    @GetMapping("/detail/{id}")
    @Transactional
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false) String lang,
                         @RequestParam(required = false) String back,
                         Model model,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String resolvedLang = resolveLang(lang, request, response);

        TourPlace place = tourPlaceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No place: " + id));

        // Initialize lazy-loaded images here
        if (place.getImages() != null) place.getImages().size();

        // Attach i18n display (wrap in List since the service takes a List)
        List<TourPlace> one = List.of(place);
        tourPlaceAutoTranslateService.ensureLocaleAndAttachDisplay(one, resolvedLang);
        codeAutoTranslateService.attachDisplayNamesForPlaces(one, resolvedLang);

        model.addAttribute("place", place);
        model.addAttribute("lang", resolvedLang);
        model.addAttribute("backUrl", normalizeBackUrl(back, resolvedLang));
        
        model.addAttribute("placeId", id);
        
        // for navigation in header.html 
        model.addAttribute("currentPage", "filter");
        
        return "detail";
    }


    
    // emergency_contacts page added
    @GetMapping("/emergency_contacts")
    public String emergency_contacts(@RequestParam(required = false) String lang,
                                      Model model,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String resolvedLang = resolveLang(lang, request, response);

        model.addAttribute("lang", resolvedLang);
        model.addAttribute("currentPage", "emergency_contacts");

        return "emergency_contacts";
    }

    // My Favorites page
    @GetMapping("/me/favorites")
    public String myFavorites(@RequestParam(required = false) String lang,
                              Model model,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        String resolvedLang = resolveLang(lang, request, response);

        model.addAttribute("lang", resolvedLang);
        model.addAttribute("currentPage", "favorites");

        return "mypage_favorites";
    }

    private String normalizeBackUrl(String back, String lang) {
        if (back == null || back.isBlank()) return null;
        if (!back.startsWith("/")) return null;
        String fragment = "";
        int hashIndex = back.indexOf('#');
        if (hashIndex >= 0) {
            fragment = back.substring(hashIndex);
            back = back.substring(0, hashIndex);
        }

        String path = back;
        String query = "";
        int queryIndex = back.indexOf('?');
        if (queryIndex >= 0) {
            path = back.substring(0, queryIndex);
            query = back.substring(queryIndex + 1);
        }

        StringBuilder rebuilt = new StringBuilder();
        boolean hasLang = false;
        if (!query.isBlank()) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.isBlank()) continue;
                if (part.startsWith("lang=")) {
                    if (rebuilt.length() > 0) rebuilt.append("&");
                    rebuilt.append("lang=").append(lang);
                    hasLang = true;
                } else {
                    if (rebuilt.length() > 0) rebuilt.append("&");
                    rebuilt.append(part);
                }
            }
        }

        if (!hasLang) {
            if (rebuilt.length() > 0) rebuilt.append("&");
            rebuilt.append("lang=").append(lang);
        }

        return path + "?" + rebuilt + fragment;
    }
    
    private String resolveLang(String lang, HttpServletRequest request, HttpServletResponse response) {
        String resolved = (lang == null || lang.isBlank()) ? "en" : lang.trim().toLowerCase();
        if (!resolved.equals("en") && !resolved.equals("ru") && !resolved.equals("tg") && !resolved.equals("ko")) {
            resolved = "en";
        }
        localeResolver.setLocale(request, response, Locale.forLanguageTag(resolved));
        return resolved;
    }
}
