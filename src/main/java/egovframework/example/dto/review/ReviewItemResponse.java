package egovframework.example.dto.review;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewItemResponse {
    private Long reviewId;
    private String nickname;
    private Integer rating;
    private String content;
    private Instant createdAt;
    private boolean mine;
}