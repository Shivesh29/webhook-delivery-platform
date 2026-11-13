package com.webhook.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${webhook.security.api-key}")
    private String configuredApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/api/v1/test-receiver");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                apiKey = authHeader.substring(7).trim();
            }
        }

        if (apiKey == null || !apiKey.equals(configuredApiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("timestamp", Instant.now().toString());
            errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
            errorDetails.put("error", "Unauthorized");
            errorDetails.put("message", "Missing or invalid API key. Provide via X-API-Key header or Authorization: Bearer <key>");

            response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
            return;
        }

        filterChain.doFilter(request, response);
    }
}

