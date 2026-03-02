package de.innologic.i18nservice.it;

import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayConnectivityIT extends IntegrationTestBase {

    @Autowired
    Flyway flyway;

    @Test
    void flywayRunsAgainstTestContainer() {
        assertThat(flyway.info().all()).isNotEmpty();
    }

    @Test
    void wrongCredentialsFailFast() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withPropertyValues(
                        "spring.datasource.url=" + jdbcUrlWithParams(),
                        "spring.datasource.username=" + DB_USERNAME,
                        "spring.datasource.password=bad-password",
                        "spring.flyway.url=" + jdbcUrlWithParams(),
                        "spring.flyway.user=" + DB_USERNAME,
                        "spring.flyway.password=bad-password")
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        FlywayAutoConfiguration.class));

        runner.run(context -> {
            Assertions.assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure)
                    .isInstanceOf(BeanCreationException.class)
                    .hasMessageContaining("Access denied for user");
        });
    }
}
