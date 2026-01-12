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
    public String home() {
        return "index"; // 홈은 검색창만 있는 단순한 페이지
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
    	
        // 드롭다운을 채울 목록 가져오기
        model.addAttribute("lang", lang);
        model.addAttribute("categories", categories);
    	model.addAttribute("regions", regions);

        // 검색어가 하나라도 있으면 검색 수행
        // 홈에서 query만 들고 와도 여기서 검색이 실행
    	// 아무것도 입력하지 않은 기본 상태일 때, 데이터 전체를 보여주기 위해 if 삭제 (if 있으면 기본 상태 시 데이터 보여주지 않음)
//        if ((query != null && !query.trim().isEmpty()) || 
//            (category != null && !category.isEmpty()) || 
//            (region != null && !region.isEmpty())) {
    	
    		// null/빈문자/공백 정리 (옵션 선택 표시 정확히)
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


        return "filter"; // src/main/resources/templates/filter.html로 이동
    }
    
    // detail page added
    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id,
                         @RequestParam(defaultValue="en") String lang,
                         Model model) {

        TourPlace place = tourPlaceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No place: " + id));

        // 이미지 lazy 로딩이면 여기서 초기화
        if (place.getImages() != null) place.getImages().size();

        // i18n display 붙이기 (서비스가 List 받으면 List로 감싸서 호출)
        List<TourPlace> one = List.of(place);
        tourPlaceAutoTranslateService.ensureLocaleAndAttachDisplay(one, lang);
        codeAutoTranslateService.attachDisplayNamesForPlaces(one, lang);

        model.addAttribute("place", place);
        model.addAttribute("lang", lang);
        
        model.addAttribute("placeId", id);
        
        // for navigation in header.html 
        model.addAttribute("currentPage", "filter");
        
        return "detail";
    }


    
    // emergency_contacts page added
    @GetMapping("/emergency_contacts")
    public String emergency_contacts(Model model) {

        model.addAttribute("currentPage", "emergency_contacts");

        return "emergency_contacts";
    }
    
}

