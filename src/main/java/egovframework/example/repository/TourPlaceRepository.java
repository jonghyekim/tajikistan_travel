package egovframework.example.repository;

import egovframework.example.domain.TourPlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Query(
            value =
            "select distinct p\n" +
            "from TourPlace p\n" +
            "join p.region r\n" +
            "join p.category c\n" +
            "left join p.i18ns i\n" +
            "where p.isActive = true\n" +
            "  and (:category is null or :category = '' or c.code = :category)\n" +
            "  and (:region   is null or :region   = '' or r.code = :region)\n" +
            "  and (\n" +
            "        :query is null or :query = '' or\n" +
            "        lower(i.title) like lower(concat('%', :query, '%')) or\n" +
            "        lower(r.code)  like lower(concat('%', :query, '%')) or\n" +
            "        lower(c.code)  like lower(concat('%', :query, '%'))\n" +
            "      )\n",
            countQuery =
            "select count(distinct p.placeId)\n" +
            "from TourPlace p\n" +
            "join p.region r\n" +
            "join p.category c\n" +
            "left join p.i18ns i\n" +
            "where p.isActive = true\n" +
            "  and (:category is null or :category = '' or c.code = :category)\n" +
            "  and (:region   is null or :region   = '' or r.code = :region)\n" +
            "  and (\n" +
            "        :query is null or :query = '' or\n" +
            "        lower(i.title) like lower(concat('%', :query, '%')) or\n" +
            "        lower(r.code)  like lower(concat('%', :query, '%')) or\n" +
            "        lower(c.code)  like lower(concat('%', :query, '%'))\n" +
            "      )\n"
    )
    Page<TourPlace> searchWithFilters(@Param("query") String query,
                                      @Param("category") String category,
                                      @Param("region") String region,
                                      Pageable pageable);
}
