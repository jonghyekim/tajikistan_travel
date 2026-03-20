package egovframework.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import egovframework.example.dto.review.ReviewRequest;
import egovframework.example.service.ReviewService;
import egovframework.example.dto.review.ReviewListResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {
	private final ReviewService reviewService;

	//리뷰 작성
    @PostMapping
    public ResponseEntity<Void> saveReview(
            @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        Long memberId = (Long) authentication.getPrincipal();
        reviewService.saveReview(memberId, request);
        return ResponseEntity.ok().build();
    }
    
    //리뷰 목록 조회
    @GetMapping("/{placeId}")
    public ResponseEntity<ReviewListResponse> getReviews(
            @PathVariable Long placeId,
            Authentication authentication
    ) {
        Long loginMemberId = null;

        if (authentication != null && authentication.getPrincipal() != null) {
            loginMemberId = (Long) authentication.getPrincipal();
        }

        return ResponseEntity.ok(reviewService.getReviews(placeId, loginMemberId));
    }
    
    //리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication
    ) {
        Long memberId = (Long) authentication.getPrincipal();
        reviewService.deleteReview(memberId, reviewId);
        return ResponseEntity.ok().build();
    }
}
