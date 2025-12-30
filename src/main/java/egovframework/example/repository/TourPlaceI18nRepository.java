package egovframework.example.repository;

import egovframework.example.domain.TourPlaceI18n;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourPlaceI18nRepository extends JpaRepository<TourPlaceI18n, Long> {

    List<TourPlaceI18n> findAllByPlace_PlaceId(Long placeId);

    Optional<TourPlaceI18n> findByPlace_PlaceIdAndLocale(Long placeId, String locale);

    boolean existsByPlace_PlaceIdAndLocale(Long placeId, String locale);

    void deleteAllByPlace_PlaceId(Long placeId);
}
