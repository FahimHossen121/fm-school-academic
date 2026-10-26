package com.example.fmschool.config;

import com.example.fmschool.repository.AccountRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AccountRepository accounts) {
        return username -> {
            var account = accounts.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
            return User.withUsername(account.getUsername())
                    .password(account.getPasswordHash())
                    .roles(account.getRole().name())
                    .build();
        };
    }

    @Bean
    public AuthenticationSuccessHandler roleLoginSuccessHandler() {
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            var handler = new SavedRequestAwareAuthenticationSuccessHandler();
            handler.setAlwaysUseDefaultTargetUrl(true);
            handler.setDefaultTargetUrl(admin ? "/" : "/me");
            handler.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/login", "/signup", "/signup/student", "/signup/teacher",
                                "/css/**", "/favicon.ico").permitAll()
                        .requestMatchers("/students", "/students/**",
                                "/api/students", "/api/students/**",
                                "/admin", "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login")
                        .successHandler(roleLoginSuccessHandler()).permitAll())
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/students", "/api/students/**",
                                "/api/teachers", "/api/teachers/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) ->
                        apiError(response, 401, "Authentication required")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                apiError(response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) ->
                                apiError(response, 403, "Access denied or missing CSRF token")))
                .build();
    }

    private static void apiError(HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
