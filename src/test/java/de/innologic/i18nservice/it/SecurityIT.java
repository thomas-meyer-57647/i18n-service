package de.innologic.i18nservice.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongAudience_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("SCOPE_i18n:read"))
                                .jwt(jwt -> jwt
                                        .issuer("https://issuer.test.local")
                                        .subject("user-123")
                                        .claim("tenant_id", "portal")
                                        .claim("jti", "jti-1")
                                        .claim("aud", java.util.List.of("other-service"))
                                        .claim("scope", "i18n:read")
                                        .issuedAt(Instant.now().minusSeconds(30))
                                        .expiresAt(Instant.now().plusSeconds(3600)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingAdminScopeOnMutatingEndpoint_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .with(readToken("portal"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"portal","displayName":"Portal"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantMismatch_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/{projectKey}/language-settings", "portal")
                        .with(readToken("another-tenant")))
                .andExpect(status().isForbidden());
    }

    @Test
    void runtimeWithoutToken_whenPublicFalse_returns401() throws Exception {
        setupRuntimeData("portal");

        mockMvc.perform(get("/api/v1/{projectKey}/translations/{lang}", "portal", "de-DE"))
                .andExpect(status().isUnauthorized());
    }

    private void setupRuntimeData(String projectKey) throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"portal","displayName":"Portal"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/{projectKey}/languages", projectKey)
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Deutsch","languageCode":"de-DE"}
                                """))
                .andExpect(status().isCreated());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bundle.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"HELLO\":\"Hallo\"}".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/{projectKey}/languages/{code}/bundle", projectKey, "de-DE")
                        .file(file)
                        .with(adminToken(projectKey)))
                .andExpect(status().isCreated());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminToken(String tenantId) {
        return tokenWithScopes(tenantId, "i18n:read", "i18n:admin");
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor readToken(String tenantId) {
        return tokenWithScopes(tenantId, "i18n:read");
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor tokenWithScopes(String tenantId, String... scopes) {
        java.util.List<GrantedAuthority> authorities = java.util.Arrays.stream(scopes)
                .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
                .toList();
        return jwt()
                .authorities(authorities)
                .jwt(jwt -> jwt
                        .issuer("https://issuer.test.local")
                        .subject("user-123")
                        .claim("jti", "jti-" + tenantId)
                        .claim("tenant_id", tenantId)
                        .claim("aud", java.util.List.of("i18n-service"))
                        .claim("scope", String.join(" ", scopes))
                        .issuedAt(Instant.now().minusSeconds(30))
                        .expiresAt(Instant.now().plusSeconds(3600)));
    }
}
