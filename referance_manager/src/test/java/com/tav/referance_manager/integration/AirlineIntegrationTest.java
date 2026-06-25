package com.tav.referance_manager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.airline.repository.AirlineRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Airline CRUD + Kafka event emission integration testleri.
 *
 * Gerçek MySQL + Kafka Testcontainer kullanılır.
 *
 * Test kapsamı:
 *  - POST → 201 + DB + Kafka CREATED event (key = AIRLINE:{code})
 *  - POST duplicate IATA code → 409 Conflict
 *  - DELETE → 204 + DB'den silindi + Kafka DELETED event
 *  - Geçersiz IATA kodu (2 büyük harf kuralı) → 400
 *  - BI_SPECIALIST ile yazma → 403
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationContainersConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.gateway.secret=test-gateway-secret",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AirlineIntegrationTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";
    private static final String TOPIC = "reference.events";


    @Value("${spring.kafka.bootstrap-servers}")
    String kafkaBootstrapServers;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AirlineRepository airlineRepository;

    private BlockingQueue<ConsumerRecord<String, String>> records;
    private KafkaMessageListenerContainer<String, String> container;

    @BeforeEach
    void setUp() {
        airlineRepository.deleteAll();
        records = new LinkedBlockingQueue<>();

        String bootstrapServers = kafkaBootstrapServers;
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "it-airline-consumer-" + System.currentTimeMillis(), "true", bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ContainerProperties containerProps = new ContainerProperties(TOPIC);
        containerProps.setMessageListener((MessageListener<String, String>) records::add);

        container = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(props), containerProps);
        container.start();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @AfterEach
    void tearDown() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    // ─── TESTLER ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Havayolu olusturma → 201 + DB kaydi + Kafka CREATED event (key=AIRLINE:TK)")
    void createAirline_returns201_persistsInDb_publishesKafkaEvent() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(
                Map.of("name", "Turkish Airlines", "code", "TK"));

        // when
        mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TK"))
                .andExpect(jsonPath("$.name").value("Turkish Airlines"));

        // then — DB
        assertThat(airlineRepository.existsByCode("TK")).isTrue();

        // then — Kafka
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka CREATED event bekleniyor").isNotNull();
        assertThat(record.key()).isEqualTo("AIRLINE:TK");
        assertThat(record.value()).contains("CREATED").contains("TK");
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Duplicate IATA kodu → 409 Conflict")
    void createAirline_duplicateCode_returns409() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(Map.of("name", "Pegasus", "code", "PC"));
        mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // when — duplicate
        mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "PegasusClone", "code", "PC"))))
                .andExpect(status().isConflict());

        // then — DB'de tek kayıt
        assertThat(airlineRepository.findAll()).hasSize(1);
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Gecersiz IATA kodu (kucuk harf) → 400 validation hatasi")
    void createAirline_invalidIataCode_returns400() throws Exception {
        mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "AirX", "code", "ax")))) // küçük harf
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Havayolu silme → 204 + DB'den silindi + Kafka DELETED event")
    void deleteAirline_returns204_removesFromDb_publishesDeletedEvent() throws Exception {
        // given
        String createResp = mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Sun Express", "code", "XQ"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();
        records.clear();

        // when
        mockMvc.perform(delete("/api/reference/airlines/" + id)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER"))
                .andExpect(status().isNoContent());

        // then — DB
        assertThat(airlineRepository.existsByCode("XQ")).isFalse();

        // then — Kafka DELETED event
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka DELETED event bekleniyor").isNotNull();
        assertThat(record.key()).isEqualTo("AIRLINE:XQ");
        assertThat(record.value()).contains("DELETED");
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "BI_SPECIALIST")
    @DisplayName("BI_SPECIALIST ile havayolu olusturma → 403")
    void createAirline_withBiSpecialist_returns403() throws Exception {
        mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "analyst")
                        .header("X-User-Roles", "ROLE_BI_SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Denied Air", "code", "DA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Havayolu guncelleme → 200 + Kafka UPDATED event")
    void updateAirline_returns200_publishesUpdatedEvent() throws Exception {
        // given
        String createResp = mockMvc.perform(post("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "AnadoluJet", "code", "AJ"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();
        records.clear();

        // when
        mockMvc.perform(put("/api/reference/airlines/" + id)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "AnadoluJet Renamed", "code", "AJ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AnadoluJet Renamed"));

        // then — Kafka
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("UPDATED");
    }
}
