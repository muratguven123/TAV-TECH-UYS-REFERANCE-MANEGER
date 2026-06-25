package com.tav.referance_manager.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * referance_manager IT testleri için Testcontainer yapılandırması.
 *
 * MySQLContainer → @ServiceConnection ile spring.datasource.* otomatik override.
 * KafkaContainer → @ServiceConnection ile spring.kafka.bootstrap-servers otomatik override.
 *
 * EmbeddedKafka kullanan mevcut testlerden (ReferenceStationIntegrationTest) bağımsızdır.
 * Bu config yalnızca @Import(IntegrationContainersConfig.class) ile açıkça import eden
 * IT sınıflarında aktif olur.
 *
 * FAS ContainersConfig pattern'ini referans alır.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationContainersConfig {

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("uys_referance_test")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
    }
}
