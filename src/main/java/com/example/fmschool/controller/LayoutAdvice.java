package com.example.fmschool.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Shared display information for the Thymeleaf navigation; authorization stays in Security/services. */
@ControllerAdvice
public class LayoutAdvice {
    @ModelAttribute("viewer")
    public Viewer viewer(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return new Viewer("", "", false, request.getRequestURI());
        }
        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("");
        return new Viewer(authentication.getName(), role, role.equals("ADMIN"), request.getRequestURI());
    }

    public record Viewer(String username, String role, boolean admin, String path) {}
}
