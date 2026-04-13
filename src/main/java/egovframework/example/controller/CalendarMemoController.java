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

import egovframework.example.dto.calendar.CalendarMemoRequestDto;
import egovframework.example.dto.calendar.CalendarMemoResponseDto;
import egovframework.example.service.CalendarMemoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/me/calendar-memo")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CalendarMemoController {

    private final CalendarMemoService calendarMemoService;

    @PostMapping("/save")
    public CalendarMemoResponseDto saveOrUpdateMemo(@RequestBody CalendarMemoRequestDto requestDto,
                                                     Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return calendarMemoService.addOrUpdateMemo(memberId, requestDto);
    }

    @GetMapping("/list")
    public List<CalendarMemoResponseDto> getMyMemos(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return calendarMemoService.getMyMemos(memberId);
    }

    @GetMapping("/date")
    public CalendarMemoResponseDto getMemoByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        return calendarMemoService.getMemoByDate(memberId, startDate);
    }

    @DeleteMapping("/{memoId}")
    public void deleteMemo(@PathVariable Long memoId,
                          Authentication authentication) {

        Long memberId = (Long) authentication.getPrincipal();
        calendarMemoService.deleteMemo(memberId, memoId);
    }
}
