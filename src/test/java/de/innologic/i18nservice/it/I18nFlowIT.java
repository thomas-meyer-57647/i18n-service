package de.innologic.i18nservice.it;

import de.innologic.i18nservice.bundle.dto.BundleMetaResponse;
import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.project.dto.CreateProjectRequest;
import de.innologic.i18nservice.project.dto.LanguageSettingsResponse;
import de.innologic.i18nservice.project.dto.ProjectScopeResponse;
import de.innologic.i18nservice.project.dto.UpdateLanguageSettingsRequest;
import de.innologic.i18nservice.translations.dto.TranslationsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class I18nFlowIT extends IntegrationTestBase {

    @org.springframework.beans.factory.annotation.Autowired
    TestRestTemplate rest;

    @Test
    void endToEnd_project_languages_settings_bundle_translations_and_guardrails() {
        String projectKey = "portal";

        // 1) Project anlegen
        ProjectScopeResponse project = rest.postForEntity(
                "/api/v1/projects",
                new CreateProjectRequest(projectKey, "Portal"),
                ProjectScopeResponse.class
        ).getBody();
        assertNotNull(project);
        assertEquals(projectKey, project.projectKey());

        // 2) Sprachen anlegen (erste Sprache setzt Default automatisch)
        LanguageResponse de = rest.postForEntity(
                "/api/v1/{projectKey}/languages",
                new CreateLanguageRequest("Deutsch", "de-DE"),
                LanguageResponse.class,
                projectKey
        ).getBody();
        assertNotNull(de);
        assertEquals("de-DE", de.languageCode());

        LanguageResponse en = rest.postForEntity(
                "/api/v1/{projectKey}/languages",
                new CreateLanguageRequest("English (US)", "en-US"),
                LanguageResponse.class,
                projectKey
        ).getBody();
        assertNotNull(en);
        assertEquals("en-US", en.languageCode());

        // 3) Settings prüfen: default sollte auto gesetzt sein
        LanguageSettingsResponse settings = rest.getForEntity(
                "/api/v1/{projectKey}/language-settings",
                LanguageSettingsResponse.class,
                projectKey
        ).getBody();
        assertNotNull(settings);
        assertEquals("de-DE", settings.defaultLanguageCode());

        // 4) Fallback setzen (expectedVersion kommt aus GET)
        LanguageSettingsResponse updatedSettings = rest.exchange(
                "/api/v1/{projectKey}/language-settings",
                HttpMethod.PUT,
                new HttpEntity<>(new UpdateLanguageSettingsRequest("de-DE", "en-US", settings.version())),
                LanguageSettingsResponse.class,
                projectKey
        ).getBody();
        assertNotNull(updatedSettings);
        assertEquals("en-US", updatedSettings.fallbackLanguageCode());

        // 5) Bundles uploaden
        BundleMetaResponse deBundle = uploadBundle(projectKey, "de-DE", "{\"A\":\"Anmelden\",\"B\":\"Abbrechen\"}");
        assertNotNull(deBundle);

        BundleMetaResponse enBundle = uploadBundle(projectKey, "en-US", "{\"A\":\"Login\",\"B\":\"Cancel\",\"C\":\"Help\"}");
        assertNotNull(enBundle);

        // 6) Translations: requested fr-FR -> resolved default de-DE, fallback en-US liefert Key C
        String url = UriComponentsBuilder.fromPath("/api/v1/{projectKey}/translations/{lang}")
                .queryParam("keys", "A,B,C")
                .buildAndExpand(projectKey, "fr-FR")
                .toUriString();

        TranslationsResponse tr = rest.getForEntity(url, TranslationsResponse.class).getBody();
        assertNotNull(tr);
        assertEquals(projectKey, tr.projectKey());
        assertEquals("fr-FR", tr.requestedLanguageCode());
        assertEquals("de-DE", tr.resolvedLanguageCode());
        assertEquals("en-US", tr.fallbackLanguageCode());

        Map<String, String> values = tr.values();
        assertEquals("Anmelden", values.get("A"));
        assertEquals("Abbrechen", values.get("B"));
        assertEquals("Help", values.get("C"));
        assertTrue(tr.missingKeys().isEmpty());
        assertFalse(tr.warnings().isEmpty());

        // 7) Guardrail: Default darf nicht gelöscht werden
        ResponseEntity<String> delDefault = rest.exchange(
                "/api/v1/{projectKey}/languages/{code}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class,
                projectKey,
                "de-DE"
        );
        assertEquals(HttpStatus.CONFLICT, delDefault.getStatusCode());

        // 8) Guardrail: Fallback darf nicht deaktiviert werden
        LanguageResponse enFresh = rest.getForEntity(
                "/api/v1/{projectKey}/languages/{code}",
                LanguageResponse.class,
                projectKey,
                "en-US"
        ).getBody();
        assertNotNull(enFresh);

        UpdateLanguageRequest deactivate = new UpdateLanguageRequest(enFresh.name(), false, enFresh.version());
        ResponseEntity<String> deactivateResp = rest.exchange(
                "/api/v1/{projectKey}/languages/{code}",
                HttpMethod.PUT,
                new HttpEntity<>(deactivate),
                String.class,
                projectKey,
                "en-US"
        );
        assertEquals(HttpStatus.CONFLICT, deactivateResp.getStatusCode());

        // 9) Bundle Download ETag: 2. Request -> 304
        ResponseEntity<byte[]> download = rest.getForEntity(
                "/api/v1/{projectKey}/languages/{code}/bundle",
                byte[].class,
                projectKey,
                "de-DE"
        );
        assertEquals(HttpStatus.OK, download.getStatusCode());
        String etag = download.getHeaders().getETag();
        assertNotNull(etag);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.IF_NONE_MATCH, etag);
        ResponseEntity<String> notModified = rest.exchange(
                "/api/v1/{projectKey}/languages/{code}/bundle",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                projectKey,
                "de-DE"
        );
        assertEquals(HttpStatus.NOT_MODIFIED, notModified.getStatusCode());
    }

    private BundleMetaResponse uploadBundle(String projectKey, String languageCode, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource file = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "bundle.json";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

        ResponseEntity<BundleMetaResponse> resp = rest.exchange(
                "/api/v1/{projectKey}/languages/{code}/bundle",
                HttpMethod.POST,
                req,
                BundleMetaResponse.class,
                projectKey,
                languageCode
        );

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }
}
