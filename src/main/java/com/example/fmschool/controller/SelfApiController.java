package com.example.fmschool.controller;

import com.example.fmschool.dto.ProfileForm;
import com.example.fmschool.service.ProfileService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SelfApiController {
    private final ProfileService profiles;

    public SelfApiController(ProfileService profiles) { this.profiles = profiles; }

    public record SessionView(String username, List<String> roles) {}
    public record TokenView(String headerName, String token) {}

    @GetMapping("/session")
    public SessionView session(Authentication authentication) {
        return new SessionView(authentication.getName(), authentication.getAuthorities().stream()
                .map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).toList());
    }

    @GetMapping("/csrf")
    public TokenView csrf(CsrfToken token) {
        return new TokenView(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public ProfileService.View me() { return profiles.getOwnProfile(); }

    @PutMapping("/me")
    public ProfileService.View update(@Valid @RequestBody ProfileForm form) {
        profiles.updateOwnProfile(form);
        return profiles.getOwnProfile();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {
        profiles.deleteOwnAccount();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
