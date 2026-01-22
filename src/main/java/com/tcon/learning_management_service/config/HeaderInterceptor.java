package com.tcon.learning_management_service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class HeaderInterceptor implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Check if X-User-Id header is missing
        if (httpRequest.getHeader("X-User-Id") == null) {
            log.warn("X-User-Id header missing, adding default value");

            // Wrap the request to add the header
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return "default-user-id";
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        List<String> values = new ArrayList<>();
                        values.add("default-user-id");
                        return Collections.enumeration(values);
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    if (!names.contains("X-User-Id")) {
                        names.add("X-User-Id");
                    }
                    return Collections.enumeration(names);
                }
            };

            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
