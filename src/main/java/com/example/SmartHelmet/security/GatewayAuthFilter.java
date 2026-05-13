package com.example.SmartHelmet.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates POSTs from the LoRa gateway via shared secret in X-Gateway-Key header.
 * Runs only on /api/gateway/** — other paths pass through unchanged.
 *
 * On success, sets an anonymous Authentication so downstream Spring Security authorization
 * (permitAll on /api/gateway/**) consistently allows the request.
 * On failure, short-circuits with 401 + JSON body.
 */
@Slf4j
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String GATEWAY_PATH_PREFIX = "/api/gateway/";
    private static final String HEADER = "X-Gateway-Key";

    private final String expectedKey;

    public GatewayAuthFilter(@Value("${gateway.api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @PostConstruct
    void logKeyState() {
        if (expectedKey == null || expectedKey.isBlank()) {
            log.error("[Gateway] GATEWAY_API_KEY 미설정 — /api/gateway/** 은 모두 401 거절됩니다");
        } else {
            log.info("[Gateway] API Key 로드 완료 (length={})", expectedKey.length());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(GATEWAY_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (expectedKey == null || expectedKey.isBlank()) {
            reject(response, "gateway api key not configured on server");
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || !provided.equals(expectedKey)) {
            log.warn("[Gateway] reject {}: missing/invalid {} header", request.getRequestURI(), HEADER);
            reject(response, "invalid gateway key");
            return;
        }

        // Mark request as authenticated so downstream authorization is satisfied.
        var auth = new AnonymousAuthenticationToken(
                "gateway",
                "gateway-device",
                List.of(new SimpleGrantedAuthority("ROLE_GATEWAY"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
