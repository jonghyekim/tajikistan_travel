package egovframework.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import egovframework.example.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	Optional<Review> findByMember_IdAndPlace_PlaceId(Long memberId, Long placeId);
}