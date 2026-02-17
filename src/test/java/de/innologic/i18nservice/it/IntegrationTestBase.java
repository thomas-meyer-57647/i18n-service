package de.innologic.i18nservice.it;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @Container
    protected static final MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:10.4.32")
            .withDatabaseName("i18n")
            .withUsername("root")
            .withPassword("");

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
        r.add("spring.datasource.url", () -> maria.getJdbcUrl() + "?useUnicode=true&characterEncoding=utf8&useSSL=false&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        r.add("spring.datasource.username", maria::getUsername);
        r.add("spring.datasource.password", maria::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

        // Bundles nicht ins Repo schreiben
        r.add("app.bundle-storage.base-path", () -> storageDir.toString());

        r.add("app.security.legacy-admin-api-key.enabled", () -> "false");
        r.add("app.security.legacy-admin-api-key.value", () -> "");
        r.add("security.jwt.issuer-uri", () -> "https://issuer.test.local");
        r.add("security.jwt.audience", () -> "i18n-service");
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
}
