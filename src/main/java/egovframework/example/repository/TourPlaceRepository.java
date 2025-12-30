package egovframework.example.repository;

import egovframework.example.domain.TourPlace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourPlaceRepository extends JpaRepository<TourPlace, Long> {

    List<TourPlace> findAllByIsActiveTrueOrderByUpdatedAtDesc();

    List<TourPlace> findAllByCategory_CodeAndIsActiveTrue(String categoryCode);

    List<TourPlace> findAllByRegion_CodeAndIsActiveTrue(String regionCode);

    List<TourPlace> findAllByCategory_CodeAndRegion_CodeAndIsActiveTrue(String categoryCode, String regionCode);

    @EntityGraph(attributePaths = {"i18ns", "images", "category", "region"})
    List<TourPlace> findAllByIsActiveTrue();
}
