package com.tcon.learning_management_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // ✅ enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtDelegatingFilter jwtDelegatingFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Security for learning-management-service");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public GET endpoints (no auth needed)
                        .requestMatchers(HttpMethod.GET,
                                "/api/courses/published",
                                "/api/courses/search",
                                "/api/grades",                        // dropdown
                                "/api/grades/*/subjects",              // dropdown
                                "/api/subjects/*/topics",              // dropdown
                                "/actuator/**",
                                "/error"
                        ).permitAll()

                        // ADMIN ONLY - write operations on master data + courses
                        .requestMatchers(HttpMethod.POST,
                                "/api/grades",
                                "/api/subjects",
                                "/api/topics",
                                "/api/courses"
                        ).hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/grades/**",
                                "/api/subjects/**",
                                "/api/topics/**",
                                "/api/courses/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/grades/**",
                                "/api/subjects/**",
                                "/api/topics/**",
                                "/api/courses/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // Everything else needs at least a valid token
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtDelegatingFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
