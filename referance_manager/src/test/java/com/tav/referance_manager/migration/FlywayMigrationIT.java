package com.tav.referance_manager.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FIX: DEF-015 — referance_manager Flyway migration doğrulama IT.
 *
 * V1: airlines, stations, aircrafts, routes tabloları + FK'lar.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.config.import=",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/jwks",
    "app.gateway.secret=test-secret",
    "spring.kafka.bootstrap-servers=localhost:9999",
    "spring.flyway.baseline-on-migrate=true",
    "spring.flyway.baseline-version=0"
})
@Import(FlywayMigrationIT.ContainersConfig.class)
class FlywayMigrationIT {

    @Configuration
    static class ContainersConfig {
        @Bean
        @ServiceConnection
        MySQLContainer<?> mysqlContainer() {
            return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("uys_referance_test")
                    .withUsername("test")
                    .withPassword("test");
        }
    }

    @Autowired
    Flyway flyway;

    @Test
    @DisplayName("DEF-015: V1 migration uygulandı — airlines/stations/aircrafts/routes mevcut")
    void flyway_v1Migration_applied() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();

        boolean v1Applied = java.util.Arrays.stream(applied)
                .anyMatch(m -> m.getVersion() != null
                        && "1".equals(m.getVersion().getVersion())
                        && m.getState().isApplied());
        assertThat(v1Applied).as("V1__baseline_schema uygulanmalı").isTrue();
    }

    @Test
    @DisplayName("DEF-015: Pending migration yok")
    void flyway_noPendingMigrations() {
        assertThat(flyway.info().pending()).isEmpty();
    }
}
