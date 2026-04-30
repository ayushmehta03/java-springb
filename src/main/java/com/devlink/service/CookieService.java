package com.devlink.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    @Value("${cookie.domain:}")
    private String domain;

    @Value("${cookie.secure:true}")
    private boolean secure;

    @Value("${cookie.same-site:None}")
    private String sameSite;

    public void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .maxAge(60 * 60 * 24)   // 1 day
                .path("/")
                .domain(domain.isBlank() ? null : domain)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .maxAge(0)
                .path("/")
                .domain(domain.isBlank() ? null : domain)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        response.setHeader("Cache-Control", "no-store");
    }
}
