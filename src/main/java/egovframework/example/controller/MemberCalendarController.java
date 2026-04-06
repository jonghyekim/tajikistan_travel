package egovframework.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/me")
public class MemberCalendarController {
    // Calendar 페이지
    @GetMapping("/calendar")
    public String calendarPage() {
        return "calendar"; // templates/calendar.html
    }
}