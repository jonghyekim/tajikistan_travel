package egovframework.example.dto.review;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewListResponse {
    private double averageRating;
    private long reviewCount;
    private List<ReviewItemResponse> reviews;
}