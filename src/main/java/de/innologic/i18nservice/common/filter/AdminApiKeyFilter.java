package de.innologic.i18nservice.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.common.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Minimaler Schutz für Admin-Endpunkte, wenn du (noch) kein Spring Security / IAM angebunden hast.
 *
 * Aktiviert sich nur, wenn {@code app.admin.api-key} gesetzt ist.
 *
 * Offene Endpunkte:
 * - Runtime: {@code /api/v1/{projectKey}/translations/{languageCode}}
 * - Bundle-Download: {@code GET /api/v1/{projectKey}/languages/{languageCode}/bundle}
 * - Bundle-Download-Version: {@code GET /api/v1/{projectKey}/languages/{languageCode}/bundle/version/{n}}
 * - Swagger/OpenAPI + Actuator
 *
 * Header:
 *  - X-Admin-Api-Key: &lt;secret&gt;
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_ADMIN_API_KEY = "X-Admin-Api-Key";

    private final String adminApiKey;
    private final ObjectMapper objectMapper;

    public AdminApiKeyFilter(
            @Value("${app.admin.api-key:}") String adminApiKey,
            ObjectMapper objectMapper
    ) {
        this.adminApiKey = adminApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // disabled by default
        if (adminApiKey == null || adminApiKey.isBlank()) {
            return true;
        }

        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String path = request.getRequestURI();

        // allow actuator & swagger
        if (path.startsWith("/actuator")) return true;
        if (path.startsWith("/v3/api-docs")) return true;
        if (path.startsWith("/swagger-ui")) return true;
        if (path.equals("/swagger-ui.html")) return true;

        // allow runtime translations
        if (path.startsWith("/api/v1/") && path.contains("/translations/")) return true;

        // allow bundle downloads (GET only)
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/") && path.contains("/languages/") && path.contains("/bundle")) {
            // e.g. /api/v1/{projectKey}/languages/{languageCode}/bundle
            // e.g. /api/v1/{projectKey}/languages/{languageCode}/bundle/version/{n}
            return true;
        }

        // protect only admin-ish endpoints
        // - /api/v1/projects/**
        // - /api/v1/{projectKey}/language-settings/**
        // - /api/v1/{projectKey}/languages/** (except GET bundle download above)
        // - /api/v1/{projectKey}/audit-logs/**
        if (path.startsWith("/api/v1/projects")) return false;
        if (path.startsWith("/api/v1/") && path.contains("/language-settings")) return false;
        if (path.startsWith("/api/v1/") && path.contains("/languages")) return false;
        if (path.startsWith("/api/v1/") && path.contains("/audit-logs")) return false;

        // everything else: no protection
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String provided = request.getHeader(HEADER_ADMIN_API_KEY);
        if (provided == null || provided.isBlank() || !provided.equals(adminApiKey)) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Unauthorized");
        pd.setDetail("Missing or invalid " + HEADER_ADMIN_API_KEY);
        pd.setProperty("requestId", RequestContext.requestId());
        pd.setProperty("actor", RequestContext.actor());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(pd));
    }
}
