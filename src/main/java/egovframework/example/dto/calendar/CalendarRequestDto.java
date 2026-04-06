package egovframework.example.dto.calendar;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarRequestDto {
    private Long placeId;
    private LocalDate startDate;
}