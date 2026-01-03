package egovframework.example.repository;

import egovframework.example.domain.TourPlace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TourPlaceRepository extends JpaRepository<TourPlace, Long> {

    List<TourPlace> findAllByIsActiveTrueOrderByUpdatedAtDesc();

    List<TourPlace> findAllByCategory_CodeAndIsActiveTrue(String categoryCode);

    List<TourPlace> findAllByRegion_CodeAndIsActiveTrue(String regionCode);

    List<TourPlace> findAllByCategory_CodeAndRegion_CodeAndIsActiveTrue(String categoryCode, String regionCode);

    @EntityGraph(attributePaths = {"i18ns", "images", "category", "region"})
    List<TourPlace> findAllByIsActiveTrue();
    
    // 관광지명(title) 또는 지역코드(regionCode)로 검색하는 쿼리
    @Query(value = "SELECT DISTINCT p.* FROM tour_place p " +
            "JOIN tour_place_i18n i ON p.place_id = i.place_id " +
            "WHERE (LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) " + // 이름 검색
            "   OR LOWER(p.region_code) LIKE LOWER(CONCAT('%', :query, '%')) " + // 지역 검색
            "   OR :query IS NULL OR :query = '') " +
            "AND (p.category_code = :category OR :category = '' OR :category IS NULL) " + // 카테고리 필터
            "AND (p.region_code = :region OR :region = '' OR :region IS NULL)", // 지역 필터
    nativeQuery = true)
    List<TourPlace> searchWithFilters(@Param("query") String query, 
                               @Param("category") String category, 
                               @Param("region") String region);
}
