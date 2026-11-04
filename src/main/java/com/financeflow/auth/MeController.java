package com.financeflow.auth;

import com.financeflow.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal UserPrincipal principal) {
        var u = principal.getUser();
        return Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "currency", u.getCurrency(),
                "theme", u.getTheme(),
                "locale", u.getLocale(),
                "timezone", u.getTimezone()
        );
    }
}