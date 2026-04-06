package egovframework.example.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import egovframework.example.dto.calendar.CalendarPlaceResponseDto;
import egovframework.example.dto.calendar.CalendarRequestDto;
import egovframework.example.service.MemberCalendarService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/me/calendar")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MemberCalendarController {

    private final MemberCalendarService memberCalendarService;

    // 일정 추가
    @PostMapping("/add")
    public void addCalendar(@RequestBody CalendarRequestDto requestDto,
                            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        memberCalendarService.addCalendar(memberId, requestDto);
    }

    // 전체 일정 조회
    @GetMapping("/list")
    public List<CalendarPlaceResponseDto> getMyCalendars(
            @RequestParam(defaultValue = "en") String lang,
            Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        return memberCalendarService.getMyCalendars(memberId, lang);
    }

    // 날짜별 일정 조회
    @GetMapping("/date")
    public List<CalendarPlaceResponseDto> getMyCalendarsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "en") String lang,
            Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        return memberCalendarService.getMyCalendarsByDate(memberId, startDate, lang);
    }

    // 일정 삭제 (calendarId 기준)
    @DeleteMapping("/delete/{calendarId}")
    public void removeCalendar(@PathVariable Long calendarId,
                               Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        memberCalendarService.removeCalendarById(memberId, calendarId);
    }

    // (선택) 캘린더에 담긴 placeId 목록만
    @GetMapping("/place-ids")
    public List<Long> getMyCalendarPlaceIds(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return memberCalendarService.getMyCalendarPlaceIds(memberId);
    }
}