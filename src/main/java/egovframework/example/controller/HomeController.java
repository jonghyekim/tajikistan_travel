package egovframework.example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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


@Controller
public class HomeController {

    @Autowired private TourPlaceRepository tourPlaceRepository;
    @Autowired private CategoryCodeRepository categoryCodeRepository;
    @Autowired private RegionCodeRepository regionCodeRepository;
    @Autowired private TourPlaceAutoTranslateService tourPlaceAutoTranslateService;
    @Autowired private CodeAutoTranslateService codeAutoTranslateService;

    @GetMapping("/")
    public String home(@RequestParam(required = false, defaultValue = "en") String lang,
                       Model model) {
        model.addAttribute("lang", lang);
        model.addAttribute("currentPage", "home");
        return "index"; // Home is a simple page with only a search box
    }
    
    // add new annotation (Transactional) for image loading
    @Transactional(readOnly = true)
    @GetMapping("/filter")
    public String filter(Model model, 
                         @RequestParam(required = false) String query,
                         @RequestParam(required = false) String category,
                         @RequestParam(required = false) String region,
                         @RequestParam(required = false, defaultValue = "en") String lang) {
        
    	List<CategoryCode> categories = categoryCodeRepository.findAll();
    	List<RegionCode> regions = regionCodeRepository.findAll();

    	codeAutoTranslateService.ensureAndAttachCategoryNames(categories, lang);
    	codeAutoTranslateService.ensureAndAttachRegionNames(regions, lang);
    	
        // Load lists to populate dropdowns
        model.addAttribute("lang", lang);
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

            
            List<TourPlace> results = tourPlaceRepository.searchWithFilters(query, category, region);
            
            // for image loading
            for (TourPlace p : results) {
                if (p.getImages() != null) {
                    p.getImages().size();
                }
            }
            
            tourPlaceAutoTranslateService.ensureLocaleAndAttachDisplay(results, lang);
            
            codeAutoTranslateService.attachDisplayNamesForPlaces(results, lang);
            
            model.addAttribute("results", results);
            
            model.addAttribute("lastQuery", query);
            model.addAttribute("lastCategory", category);
            model.addAttribute("lastRegion", region);
            
            // for navigation in header.html 
            model.addAttribute("currentPage", "filter");

//        }


        return "filter"; // Navigate to src/main/resources/templates/filter.html
    }
    
    // detail page added
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id,
                         @RequestParam(defaultValue="en") String lang,
                         @RequestParam(required = false) String back,
                         Model model) {

        TourPlace place = tourPlaceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No place: " + id));

        // Initialize lazy-loaded images here
        if (place.getImages() != null) place.getImages().size();

        // Attach i18n display (wrap in List since the service takes a List)
        List<TourPlace> one = List.of(place);
        tourPlaceAutoTranslateService.ensureLocaleAndAttachDisplay(one, lang);
        codeAutoTranslateService.attachDisplayNamesForPlaces(one, lang);

        model.addAttribute("place", place);
        model.addAttribute("lang", lang);
        model.addAttribute("backUrl", normalizeBackUrl(back, lang));
        
        model.addAttribute("placeId", id);
        
        // for navigation in header.html 
        model.addAttribute("currentPage", "filter");
        
        return "detail";
    }


    
    // emergency_contacts page added
    @GetMapping("/emergency_contacts")
    public String emergency_contacts(@RequestParam(required = false, defaultValue = "en") String lang,
                                      Model model) {

        model.addAttribute("lang", lang);
        model.addAttribute("currentPage", "emergency_contacts");

        return "emergency_contacts";
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
    
}
