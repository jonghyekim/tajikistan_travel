package egovframework.example.dto.calendar;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalendarPlaceResponseDto {

    private Long calendarId;

    private Long placeId;
    private String title;
    private String content;
    private String imageUrl;
    private String categoryName;
    private String regionName;

    private LocalDate startDate;
}