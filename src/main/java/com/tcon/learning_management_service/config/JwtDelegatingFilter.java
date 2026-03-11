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

        // ✅ Skip public GET endpoints entirely
        boolean isPublicGet = "GET".equalsIgnoreCase(method) && (
                path.startsWith("/api/courses/published") ||
                        path.startsWith("/api/courses/search") ||
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

        // ✅ FIX: Read user info from gateway-injected headers (no Feign call needed)
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");
        String email  = request.getHeader("X-User-Email");

        // If headers are missing, fall back to checking Authorization header exists
        // (for direct service-to-service calls without gateway)
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(role)) {
            log.warn("Missing X-User-Id or X-User-Role headers for path: {} {}", method, path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing user identity headers\"}");
            return;
        }

        // ✅ FIX: Use actual role from header, not hardcoded ROLE_ADMIN
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
