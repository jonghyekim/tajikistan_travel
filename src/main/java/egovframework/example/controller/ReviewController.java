package egovframework.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import egovframework.example.dto.review.ReviewRequest;
import egovframework.example.service.ReviewService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {
	private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Void> saveReview(
            @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        Long memberId = (Long) authentication.getPrincipal();
        reviewService.saveReview(memberId, request);
        return ResponseEntity.ok().build();
    }
}
