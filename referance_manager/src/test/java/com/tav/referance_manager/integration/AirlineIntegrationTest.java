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
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"reference.events"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AirlineIntegrationTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";
    private static final String TOPIC = "reference.events";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

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

        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "it-airline-consumer-" + System.currentTimeMillis(), "true", embeddedKafkaBroker);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ContainerProperties containerProps = new ContainerProperties(TOPIC);
        containerProps.setMessageListener((MessageListener<String, String>) records::add);

        container = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(props), containerProps);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    private ConsumerRecord<String, String> pollRecord(String key, String valueSubstring) throws InterruptedException {
        long stopTime = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < stopTime) {
            ConsumerRecord<String, String> record = records.poll(100, TimeUnit.MILLISECONDS);
            if (record != null && key.equals(record.key()) && (valueSubstring == null || record.value().contains(valueSubstring))) {
                return record;
            }
        }
        return null;
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
        ConsumerRecord<String, String> record = pollRecord("AIRLINE:TK", "CREATED");
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
        pollRecord("AIRLINE:XQ", "CREATED"); // wait for CREATED event to be fully received

        // when
        mockMvc.perform(delete("/api/reference/airlines/" + id)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER"))
                .andExpect(status().isNoContent());

        // then — DB
        assertThat(airlineRepository.existsByCode("XQ")).isFalse();

        // then — Kafka DELETED event
        ConsumerRecord<String, String> record = pollRecord("AIRLINE:XQ", "DELETED");
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
        pollRecord("AIRLINE:AJ", "CREATED"); // wait for CREATED event to be fully received

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
        ConsumerRecord<String, String> record = pollRecord("AIRLINE:AJ", "UPDATED");
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("UPDATED");
    }

    @Test
    @Order(7)
    @WithMockUser(roles = "BI_SPECIALIST")
    @DisplayName("GET /api/reference/airlines - BI_SPECIALIST ile okuma -> 200 OK")
    void listAirlines_withBiSpecialist_returns200() throws Exception {
        mockMvc.perform(get("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "analyst")
                        .header("X-User-Roles", "ROLE_BI_SPECIALIST"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/reference/airlines - Gecersiz rol (USER) ile okuma -> 403 Forbidden")
    void listAirlines_withUser_returns403() throws Exception {
        mockMvc.perform(get("/api/reference/airlines")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "common-user")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Yetkisiz işlem"));
    }
}
