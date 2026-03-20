package egovframework.example.dto.review;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private Long placeId;
    private Integer rating;
    private String content;
}