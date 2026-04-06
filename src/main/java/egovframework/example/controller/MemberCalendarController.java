package egovframework.example.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.servlet.LocaleResolver;

@Controller
@RequestMapping("/me")
public class MemberCalendarController {
    @Autowired private LocaleResolver localeResolver;

    // Calendar 페이지
    @GetMapping("/calendar")
    public String calendarPage(@RequestParam(required = false) String lang,
                               Model model,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        model.addAttribute("lang", resolveLang(lang, request, response));
        model.addAttribute("currentPage", "calendar");
        return "calendar"; // templates/calendar.html
    }

    private String resolveLang(String lang, HttpServletRequest request, HttpServletResponse response) {
        String resolved = normalizeLang(lang);
        if (resolved == null) {
            Locale currentLocale = localeResolver.resolveLocale(request);
            resolved = normalizeLang(currentLocale != null ? currentLocale.toLanguageTag() : null);
        }
        if (resolved == null) {
            resolved = "en";
        }

        localeResolver.setLocale(request, response, Locale.forLanguageTag(resolved));
        return resolved;
    }

    private String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return null;
        }

        String normalized = lang.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("-")) {
            normalized = normalized.substring(0, normalized.indexOf('-'));
        }

        if (normalized.equals("en") || normalized.equals("ru") || normalized.equals("tg") || normalized.equals("ko")) {
            return normalized;
        }
        return null;
    }
}
