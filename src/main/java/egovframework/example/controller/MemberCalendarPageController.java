package egovframework.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/me")
public class MemberCalendarPageController {

    @GetMapping("/calendar")
    public String calendarPage() {
        return "calendar";
    }
}