package de.innologic.i18nservice.it;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(IntegrationTestBase.JwtTestConfiguration.class)
public abstract class IntegrationTestBase {

    protected static final String DB_NAME = "i18n";
    protected static final String DB_USERNAME = "i18n_user";
    protected static final String DB_PASSWORD = "i18n_pass";
    private static final String JDBC_PARAMS = "?useUnicode=true&characterEncoding=utf8&useSSL=false&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";
    private static final byte[] TEST_JWT_SECRET = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);

    @Container
    protected static final MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:10.4.32")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USERNAME)
            .withPassword(DB_PASSWORD);

    private static final Path storageDir;

    static {
        try {
            storageDir = Files.createTempDirectory("i18n-bundles-");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", IntegrationTestBase::jdbcUrlWithParams);
        r.add("spring.datasource.username", () -> DB_USERNAME);
        r.add("spring.datasource.password", () -> DB_PASSWORD);
        r.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

        // Bundles nicht ins Repo schreiben
        r.add("app.bundle-storage.base-path", () -> storageDir.toString());

        r.add("app.security.legacy-admin-api-key.enabled", () -> "false");
        r.add("app.security.legacy-admin-api-key.value", () -> "");
        r.add("security.jwt.issuer-uri", () -> "https://issuer.test.local");
        r.add("security.jwt.audience", () -> "i18n-service");
        r.add("spring.flyway.url", IntegrationTestBase::jdbcUrlWithParams);
        r.add("spring.flyway.user", () -> DB_USERNAME);
        r.add("spring.flyway.password", () -> DB_PASSWORD);
        r.add("spring.main.allow-bean-definition-overriding", () -> "true");
    }

    protected static String jdbcUrlWithParams() {
        return maria.getJdbcUrl() + JDBC_PARAMS;
    }

    @AfterAll
    static void cleanup() {
        try {
            Files.walk(storageDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {
        }
    }

    @TestConfiguration
    static class JwtTestConfiguration {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            SecretKey key = new SecretKeySpec(TEST_JWT_SECRET, "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(key).build();
        }
    }
}
