package egovframework.example.security;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenResolver {

    // 쿠키(refreshToken) 우선, 없으면 바디 토큰 사용
    public String resolve(HttpServletRequest request, String bodyToken) {
        String fromCookie = findCookie(request, "refreshToken");
        if (fromCookie != null && !fromCookie.isBlank()) return fromCookie;

        if (bodyToken != null && !bodyToken.isBlank()) return bodyToken;

        return null;
    }

    private String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}