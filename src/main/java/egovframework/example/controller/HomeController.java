package egovframework.example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import egovframework.example.domain.CategoryCode;
import egovframework.example.domain.RegionCode;
import egovframework.example.domain.TourPlace;
import egovframework.example.repository.CategoryCodeRepository;
import egovframework.example.repository.RegionCodeRepository;
import egovframework.example.repository.TourPlaceRepository;
import egovframework.example.service.CodeAutoTranslateService;
import egovframework.example.service.TourPlaceAutoTranslateService;

@Controller
public class HomeController {

    @Autowired private TourPlaceRepository tourPlaceRepository;
    @Autowired private CategoryCodeRepository categoryCodeRepository;
    @Autowired private RegionCodeRepository regionCodeRepository;
    @Autowired private TourPlaceAutoTranslateService tourPlaceAutoTranslateService;
    @Autowired private CodeAutoTranslateService codeAutoTranslateService;

    @GetMapping("/")
    public String home() {
        return "home"; // 홈은 검색창만 있는 단순한 페이지
    }
    
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
    	
        // 드롭다운을 채울 목록 가져오기
        model.addAttribute("lang", lang);
        model.addAttribute("categories", categories);
    	model.addAttribute("regions", regions);

        // 검색어가 하나라도 있으면 검색 수행
        // 홈에서 query만 들고 와도 여기서 검색이 실행
        if ((query != null && !query.trim().isEmpty()) || 
            (category != null && !category.isEmpty()) || 
            (region != null && !region.isEmpty())) {
            
            List<TourPlace> results = tourPlaceRepository.searchWithFilters(query, category, region);
            
            tourPlaceAutoTranslateService.ensureLocaleAndAttachDisplay(results, lang);
            
            codeAutoTranslateService.attachDisplayNamesForPlaces(results, lang);
            
            model.addAttribute("results", results);
        }

        // 사용자가 입력했던 값들을 다시 화면에 전달 (상태 유지)
        model.addAttribute("lastQuery", query);
        model.addAttribute("lastCategory", category);
        model.addAttribute("lastRegion", region);

        return "filter"; // src/main/resources/templates/filter.html로 이동
    }
}
