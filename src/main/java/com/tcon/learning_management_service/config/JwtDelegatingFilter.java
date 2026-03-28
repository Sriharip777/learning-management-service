package com.tcon.learning_management_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtDelegatingFilter extends OncePerRequestFilter {

    // ✅ FIX: No longer inject AuthServiceClient — gateway already validated JWT
    // and forwarded X-User-Id, X-User-Role, X-User-Email headers

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // Public GET endpoints – no auth required, just continue
        boolean isPublicGet = "GET".equalsIgnoreCase(method) && (
                path.startsWith("/api/courses/published") ||
                        path.startsWith("/api/courses/public/published") ||
                        path.startsWith("/api/courses/search")    ||
                        path.startsWith("/api/courses/popular")   ||
                        path.equals("/api/grades") ||
                        path.matches("/api/grades/[^/]+/subjects") ||
                        path.matches("/api/subjects/[^/]+/topics") ||
                        path.startsWith("/actuator") ||
                        path.startsWith("/error")
        );

        if (isPublicGet) {
            filterChain.doFilter(request, response);
            return;
        }

        // Read user info from gateway headers
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");
        String email  = request.getHeader("X-User-Email");

        // If no user headers → treat as anonymous, let Spring Security decide
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(role)) {
            log.debug("Anonymous request for path: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        log.info("✅ Authenticated from gateway headers: userId={}, role={}, email={}", userId, authority, email);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

}