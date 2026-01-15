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
    
    // Query to search by tourist place title or region code
    @Query("""
    		select distinct p
    		from TourPlace p
    		join fetch p.region r
    		join fetch p.category c
    		left join fetch p.i18ns i
    		where p.isActive = true
    		  and (:category is null or :category = '' or c.code = :category)
    		  and (:region   is null or :region   = '' or r.code = :region)
    		  and (
    		        :query is null or :query = '' or
    		        lower(i.title) like lower(concat('%', :query, '%')) or
    		        lower(r.code)  like lower(concat('%', :query, '%')) or
    		        lower(c.code)  like lower(concat('%', :query, '%'))
    		      )
    		""")
    List<TourPlace> searchWithFilters(@Param("query") String query,
						            @Param("category") String category,
						            @Param("region") String region);
}
