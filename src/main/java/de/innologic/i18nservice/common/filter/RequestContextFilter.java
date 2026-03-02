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
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_TRACE_PARENT = "traceparent";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = determineRequestId(request);

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

    private String determineRequestId(HttpServletRequest request) {
        String requestId = trimToNull(request.getHeader(HEADER_REQUEST_ID));
        if (requestId != null) {
            return requestId;
        }
        requestId = trimToNull(request.getHeader(HEADER_CORRELATION_ID));
        if (requestId != null) {
            return requestId;
        }
        String traceParent = trimToNull(request.getHeader(HEADER_TRACE_PARENT));
        if (traceParent != null) {
            return extractTraceId(traceParent);
        }
        return UUID.randomUUID().toString();
    }

    private String extractTraceId(String traceParent) {
        String[] parts = traceParent.split("-");
        if (parts.length >= 2 && isTraceId(parts[1])) {
            return parts[1];
        }
        return traceParent;
    }

    private boolean isTraceId(String value) {
        if (value.length() != 32) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i)) && (Character.toLowerCase(value.charAt(i)) < 'a' || Character.toLowerCase(value.charAt(i)) > 'f')) {
                return false;
            }
        }
        return true;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
