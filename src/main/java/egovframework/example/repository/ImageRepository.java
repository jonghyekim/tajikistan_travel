package egovframework.example.repository;

import egovframework.example.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findAllByPlace_PlaceIdOrderByCreatedAtDesc(Long placeId);

    void deleteAllByPlace_PlaceId(Long placeId);
}
