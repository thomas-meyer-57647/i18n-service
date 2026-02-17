package de.innologic.i18nservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtPrincipalAccessor {

    public String currentSubject() {
        Jwt jwt = currentJwt();
        return jwt != null ? jwt.getSubject() : null;
    }

    public String currentTenantId() {
        Jwt jwt = currentJwt();
        return jwt != null ? jwt.getClaimAsString("tenant_id") : null;
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        return null;
    }
}
