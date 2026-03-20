package egovframework.example.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.example.domain.Member;
import egovframework.example.domain.Review;
import egovframework.example.domain.TourPlace;
import egovframework.example.dto.review.ReviewItemResponse;
import egovframework.example.dto.review.ReviewListResponse;
import egovframework.example.dto.review.ReviewRequest;
import egovframework.example.repository.MemberRepository;
import egovframework.example.repository.ReviewRepository;
import egovframework.example.repository.TourPlaceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
	private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
	private final TourPlaceRepository tourPlaceRepository;

	//리뷰 작성 => 저장
	@Transactional
	public void saveReview(Long memberId, ReviewRequest request) {
		validate(request);
		
		Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));

        TourPlace place = tourPlaceRepository.findById(request.getPlaceId())
                .orElseThrow(() -> new IllegalArgumentException("place not found"));
        
        Review review = reviewRepository.findByMember_IdAndPlace_PlaceId(memberId, request.getPlaceId())
                .orElseGet(() -> {
                    Review newReview = new Review();
                    newReview.setMember(member);
                    newReview.setPlace(place);
                    return newReview;
                });
        
        review.setRating(request.getRating());
        review.setContent(trimToNull(request.getContent()));

        reviewRepository.save(review);
        
	}

	//리뷰 유효성 검증
	private void validate(ReviewRequest request) {
        if (request.getPlaceId() == null) {
            throw new IllegalArgumentException("placeId is required");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }

        if (request.getContent() != null && request.getContent().length() > 1000) {
            throw new IllegalArgumentException("content is too long");
        }
    }
	
	//리뷰 작성시 공백만 입력할 경우 null처리
	private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
	
	//작성된 리뷰 리스트 조회
	@Transactional(readOnly = true)
	public ReviewListResponse getReviews(Long placeId, Long loginMemberId) {
	    List<Review> reviews = reviewRepository.findByPlace_PlaceIdOrderByCreatedAtDesc(placeId);

	    double averageRating = reviewRepository.findAverageRatingByPlaceId(placeId);
	    long reviewCount = reviewRepository.countByPlace_PlaceId(placeId);

	    List<ReviewItemResponse> items = reviews.stream()
	            .map(review -> new ReviewItemResponse(
	                    review.getId(),
	                    review.getMember().getNickname(),
	                    review.getRating(),
	                    review.getContent(),
	                    review.getCreatedAt(),
	                    loginMemberId != null && loginMemberId.equals(review.getMember().getId())
	            ))
	            .collect(Collectors.toList());

	    return new ReviewListResponse(averageRating, reviewCount, items);
	}
	
	//본인이 작성한 리 삭제
	@Transactional
	public void deleteReview(Long memberId, Long reviewId) {
	    Review review = reviewRepository.findById(reviewId)
	            .orElseThrow(() -> new IllegalArgumentException("review not found"));

	    if (!review.getMember().getId().equals(memberId)) {
	        throw new IllegalArgumentException("you can delete only your review");
	    }

	    reviewRepository.delete(review);
	}
	
}
