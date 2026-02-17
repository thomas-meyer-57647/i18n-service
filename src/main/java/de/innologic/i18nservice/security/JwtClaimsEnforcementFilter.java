package de.innologic.i18nservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtClaimsEnforcementFilter extends OncePerRequestFilter {

    private final JwtSecurityProperties properties;
    private final ProblemResponseWriter problemResponseWriter;

    public JwtClaimsEnforcementFilter(JwtSecurityProperties properties, ProblemResponseWriter problemResponseWriter) {
        this.properties = properties;
        this.problemResponseWriter = problemResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = jwtAuthenticationToken.getToken();

        if (isBlank(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null)) {
            problemResponseWriter.unauthorized(response, "Missing required claim: iss");
            return;
        }
        if (isBlank(jwt.getSubject())) {
            problemResponseWriter.unauthorized(response, "Missing required claim: sub");
            return;
        }
        if (isBlank(jwt.getClaimAsString("jti"))) {
            problemResponseWriter.unauthorized(response, "Missing required claim: jti");
            return;
        }
        if (isBlank(jwt.getClaimAsString("tenant_id"))) {
            problemResponseWriter.unauthorized(response, "Missing required claim: tenant_id");
            return;
        }
        if (jwt.getIssuedAt() == null) {
            problemResponseWriter.unauthorized(response, "Missing required claim: iat");
            return;
        }
        if (jwt.getExpiresAt() == null) {
            problemResponseWriter.unauthorized(response, "Missing required claim: exp");
            return;
        }

        List<String> aud = jwt.getAudience();
        if (aud == null || aud.stream().noneMatch(properties.getAudience()::equals)) {
            problemResponseWriter.unauthorized(response, "Invalid audience");
            return;
        }

        String configuredIssuer = trimToNull(properties.getIssuerUri());
        if (configuredIssuer == null) {
            configuredIssuer = trimToNull(properties.getIssuer());
        }
        if (configuredIssuer != null && !configuredIssuer.equals(jwt.getIssuer().toString())) {
            problemResponseWriter.unauthorized(response, "Invalid issuer");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
