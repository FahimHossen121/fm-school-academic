package com.example.fmschool.config;

import com.example.fmschool.repository.AccountRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/login", "/css/**", "/favicon.ico").permitAll()
                        .requestMatchers("/students", "/students/**",
                                "/api/students", "/api/students/**",
                                "/admin", "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login")
                        .defaultSuccessUrl("/", true).permitAll())
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
