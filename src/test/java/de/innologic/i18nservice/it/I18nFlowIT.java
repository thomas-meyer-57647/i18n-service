package de.innologic.i18nservice.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class I18nFlowIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void endToEnd_project_languages_settings_bundle_translations_and_guardrails() throws Exception {
        String projectKey = "portal";

        mockMvc.perform(post("/api/v1/projects")
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"portal","displayName":"Portal"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectKey", is(projectKey)));

        mockMvc.perform(post("/api/v1/{projectKey}/languages", projectKey)
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Deutsch","languageCode":"de-DE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode", is("de-DE")));

        mockMvc.perform(post("/api/v1/{projectKey}/languages", projectKey)
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"English (US)","languageCode":"en-US"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.languageCode", is("en-US")));

        MvcResult settingsResult = mockMvc.perform(get("/api/v1/{projectKey}/language-settings", projectKey)
                        .with(readToken(projectKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultLanguageCode", is("de-DE")))
                .andReturn();

        JsonNode settingsJson = objectMapper.readTree(settingsResult.getResponse().getContentAsString());
        long expectedVersion = settingsJson.path("version").asLong();

        mockMvc.perform(put("/api/v1/{projectKey}/language-settings", projectKey)
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"defaultLanguageCode":"de-DE","fallbackLanguageCode":"en-US","expectedVersion":%d}
                                """.formatted(expectedVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallbackLanguageCode", is("en-US")));

        uploadBundle(projectKey, "de-DE", "{\"A\":\"Anmelden\",\"B\":\"Abbrechen\"}");
        uploadBundle(projectKey, "en-US", "{\"A\":\"Login\",\"B\":\"Cancel\",\"C\":\"Help\"}");

        mockMvc.perform(get("/api/v1/{projectKey}/translations/{lang}", projectKey, "fr-FR")
                        .with(readToken(projectKey))
                        .param("keys", "A,B,C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedLanguageCode", is("fr-FR")))
                .andExpect(jsonPath("$.resolvedLanguageCode", is("de-DE")))
                .andExpect(jsonPath("$.fallbackLanguageCode", is("en-US")))
                .andExpect(jsonPath("$.values.A", is("Anmelden")))
                .andExpect(jsonPath("$.values.B", is("Abbrechen")))
                .andExpect(jsonPath("$.values.C", is("Help")));

        mockMvc.perform(delete("/api/v1/{projectKey}/languages/{code}", projectKey, "de-DE")
                        .with(adminToken(projectKey)))
                .andExpect(status().isConflict());

        MvcResult enResult = mockMvc.perform(get("/api/v1/{projectKey}/languages/{code}", projectKey, "en-US")
                        .with(readToken(projectKey)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode en = objectMapper.readTree(enResult.getResponse().getContentAsString());

        mockMvc.perform(put("/api/v1/{projectKey}/languages/{code}", projectKey, "en-US")
                        .with(adminToken(projectKey))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","active":false,"expectedVersion":%d}
                                """.formatted(en.path("name").asText(), en.path("version").asLong())))
                .andExpect(status().isConflict());

        MvcResult download = mockMvc.perform(get("/api/v1/{projectKey}/languages/{code}/bundle", projectKey, "de-DE")
                        .with(readToken(projectKey)))
                .andExpect(status().isOk())
                .andReturn();
        String etag = download.getResponse().getHeader(HttpHeaders.ETAG);
        assertNotNull(etag);

        mockMvc.perform(get("/api/v1/{projectKey}/languages/{code}/bundle", projectKey, "de-DE")
                        .with(readToken(projectKey))
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }

    private void uploadBundle(String projectKey, String languageCode, String json) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bundle.json",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/{projectKey}/languages/{code}/bundle", projectKey, languageCode)
                        .file(file)
                        .with(adminToken(projectKey)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"));
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
