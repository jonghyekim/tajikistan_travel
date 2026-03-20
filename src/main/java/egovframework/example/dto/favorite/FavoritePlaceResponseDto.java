package egovframework.example.dto.favorite;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FavoritePlaceResponseDto {

    private Long placeId;
    private String title;
    private String content;
    private String imageUrl;
    private String categoryName;
    private String regionName;
}