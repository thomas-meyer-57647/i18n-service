package de.innologic.i18nservice.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityRuntimePublicIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void runtimePublic(DynamicPropertyRegistry r) {
        r.add("app.runtime.public", () -> "true");
    }

    @Test
    void runtimeWithoutToken_whenPublicTrue_returns200() throws Exception {
        String projectKey = "portal";

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

        mockMvc.perform(get("/api/v1/{projectKey}/translations/{lang}", projectKey, "de-DE"))
                .andExpect(status().isOk());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminToken(String tenantId) {
        return jwt()
                .authorities(
                        new SimpleGrantedAuthority("SCOPE_i18n:read"),
                        new SimpleGrantedAuthority("SCOPE_i18n:admin")
                )
                .jwt(jwt -> jwt
                        .issuer("https://issuer.test.local")
                        .subject("user-123")
                        .claim("jti", "jti-" + tenantId)
                        .claim("tenant_id", tenantId)
                        .claim("aud", java.util.List.of("i18n-service"))
                        .claim("scope", "i18n:read i18n:admin")
                        .issuedAt(Instant.now().minusSeconds(30))
                        .expiresAt(Instant.now().plusSeconds(3600)));
    }
}
