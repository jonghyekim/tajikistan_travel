package egovframework.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/me")
public class MemberCalendarPageController {

    @GetMapping("/calendar")
    public String calendarPage(Model model) {
        model.addAttribute("currentPage", "calendar");
        return "calendar";
    }
}