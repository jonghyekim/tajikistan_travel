package egovframework.example.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/add")
    public void addCalendar(@RequestBody CalendarRequestDto requestDto,
                            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        memberCalendarService.addCalendar(memberId, requestDto);
    }

    @GetMapping("/list")
    public List<CalendarPlaceResponseDto> getMyCalendars(
            @RequestParam(defaultValue = "en") String lang,
            Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        return memberCalendarService.getMyCalendars(memberId, lang);
    }

    @GetMapping("/date")
    public List<CalendarPlaceResponseDto> getMyCalendarsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "en") String lang,
            Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        return memberCalendarService.getMyCalendarsByDate(memberId, startDate, lang);
    }

    @DeleteMapping("/delete/{calendarId}")
    public void removeCalendar(@PathVariable Long calendarId,
                               Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        memberCalendarService.removeCalendarById(memberId, calendarId);
    }

    @GetMapping("/place-ids")
    public List<Long> getMyCalendarPlaceIds(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return memberCalendarService.getMyCalendarPlaceIds(memberId);
    }
}
