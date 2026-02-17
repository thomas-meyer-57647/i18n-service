package de.innologic.i18nservice.common.filter;

import de.innologic.i18nservice.common.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Setzt pro Request:
 * - requestId (aus Header X-Request-Id oder generiert)
 *
 * Zusätzlich:
 * - schreibt requestId als Response Header zurück
 * - legt requestId in MDC ab (für Logs)
 *
 * WICHTIG: Bean-Name darf nicht "requestContextFilter" heißen,
 * weil Spring Boot selbst bereits ein Bean mit diesem Namen registriert.
 */
@Component("i18nRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = trimToNull(request.getHeader(HEADER_REQUEST_ID));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        RequestContext.setRequestId(requestId);
        MDC.put("requestId", requestId);

        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("actor");
            RequestContext.clear();
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
