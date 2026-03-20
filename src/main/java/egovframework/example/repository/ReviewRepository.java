package egovframework.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import egovframework.example.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	//사용자가 해당 장소에 대한 리뷰를 이미 남겼는지, 아닌지 조회
	Optional<Review> findByMember_IdAndPlace_PlaceId(Long memberId, Long placeId);
	
	//해당 장소 리뷰 최신순 조회
	List<Review> findByPlace_PlaceIdOrderByCreatedAtDesc(Long placeId);

	//리뷰 개수 조회
    long countByPlace_PlaceId(Long placeId);

    //평균 평점 조회
    @Query("select coalesce(avg(r.rating), 0) from Review r where r.place.placeId = :placeId")
    Double findAverageRatingByPlaceId(@Param("placeId") Long placeId);
}