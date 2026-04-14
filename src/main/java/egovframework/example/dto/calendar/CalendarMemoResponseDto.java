package egovframework.example.dto.calendar;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalendarMemoResponseDto {
    private Long memoId;
    private LocalDate startDate;
    private String memo;
}
