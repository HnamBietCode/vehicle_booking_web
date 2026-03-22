package com.bookvehicle.example.sr.config;

import com.bookvehicle.example.sr.model.User;
import com.bookvehicle.example.sr.service.MobileAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {

    private final MobileAuthService mobileAuthService;

    public ApiAuthFilter(MobileAuthService mobileAuthService) {
        this.mobileAuthService = mobileAuthService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/mobile/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String raw = auth.substring(7);
            User user = mobileAuthService.resolveUser(raw);
            if (user != null) {
                request.setAttribute(SecurityUtil.SESSION_KEY, user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
