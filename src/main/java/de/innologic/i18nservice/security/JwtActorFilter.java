package de.innologic.i18nservice.security;

import de.innologic.i18nservice.common.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtActorFilter extends OncePerRequestFilter {

    private final JwtPrincipalAccessor principalAccessor;

    public JwtActorFilter(JwtPrincipalAccessor principalAccessor) {
        this.principalAccessor = principalAccessor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sub = principalAccessor.currentSubject();
        if (sub != null && !sub.isBlank()) {
            RequestContext.setActor(sub);
            MDC.put("actor", sub);
        }

        filterChain.doFilter(request, response);
    }
}
