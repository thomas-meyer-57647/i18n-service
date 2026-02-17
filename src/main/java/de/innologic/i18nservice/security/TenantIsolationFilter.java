package de.innologic.i18nservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TenantIsolationFilter extends OncePerRequestFilter {

    private static final Pattern API_PROJECT_KEY_PATTERN = Pattern.compile("^/api/v1/([^/]+)/.*$");
    private static final Pattern PROJECTS_PROJECT_KEY_PATTERN = Pattern.compile("^/api/v1/projects/([^/]+)$");

    private final JwtPrincipalAccessor principalAccessor;
    private final ProblemResponseWriter problemResponseWriter;
    private final boolean runtimePublic;

    public TenantIsolationFilter(JwtPrincipalAccessor principalAccessor,
                                 ProblemResponseWriter problemResponseWriter,
                                 boolean runtimePublic) {
        this.principalAccessor = principalAccessor;
        this.problemResponseWriter = problemResponseWriter;
        this.runtimePublic = runtimePublic;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String projectKey = extractProjectKey(request.getRequestURI());
        if (projectKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            if (runtimePublic && isRuntimeReadPath(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = principalAccessor.currentTenantId();
        if (tenantId == null || !tenantId.equals(projectKey)) {
            problemResponseWriter.forbidden(response, "Tenant mismatch: projectKey must match token tenant_id");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String extractProjectKey(String uri) {
        Matcher projectByPrefix = API_PROJECT_KEY_PATTERN.matcher(uri);
        if (projectByPrefix.matches()) {
            String firstSegment = projectByPrefix.group(1);
            if (!"projects".equals(firstSegment)) {
                return firstSegment;
            }
        }

        Matcher projectByProjectsRoute = PROJECTS_PROJECT_KEY_PATTERN.matcher(uri);
        if (projectByProjectsRoute.matches()) {
            return projectByProjectsRoute.group(1);
        }

        return null;
    }

    private static boolean isRuntimeReadPath(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/api/v1/") && uri.contains("/translations/");
    }
}
