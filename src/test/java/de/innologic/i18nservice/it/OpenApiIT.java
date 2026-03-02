package de.innologic.i18nservice.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Test
    void apiDocsExposeProjectPathsWithResponsesAndSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/projects']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].post.tags[0]").value("Project Scopes"))
                .andExpect(jsonPath("$.paths['/api/v1/projects'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/language-settings']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/language-settings'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/language-settings'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/meta']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/versions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/version/{version}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/version/{version}'].get.responses['200']").exists());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/translations/{languageCode}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/translations/{languageCode}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/translations/{languageCode}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/translations/{languageCode}'].get.security[0].bearerAuth").isArray());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/diff']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/diff'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/diff'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/diff'].get.responses['403']").exists());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/audit-logs']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/audit-logs'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/audit-logs'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/audit-logs'].get.responses['403']").exists());
    }

    @Test
    void apiDocsDoesNotContainUnknownProjectPath() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/projects/does-not-exist']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/unknown']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/language-settings'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle'].put").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/languages/{languageCode}/bundle/diff'].post").doesNotExist());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/translations'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/audit-logs'].post").doesNotExist());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/{projectKey}/translations'].post").doesNotExist());
    }
}
