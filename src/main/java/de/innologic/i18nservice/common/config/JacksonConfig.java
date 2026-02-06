package de.innologic.i18nservice.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // registriert automatisch z.B. JavaTimeModule (für Instant etc.),
        // sofern die Module auf dem Classpath sind.
        return new ObjectMapper().findAndRegisterModules();
    }
}
