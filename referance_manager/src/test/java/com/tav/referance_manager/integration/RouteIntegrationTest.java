package com.tav.referance_manager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.route.repository.RouteRepository;
import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.repository.StationRepository;
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
 * Route CRUD + Kafka event emission integration testleri.
 *
 * Route entity'si iki Station'a bağımlıdır (originStation, destinationStation).
 * Testler önce Station'ları DB'ye yazar, ardından Route operasyonlarını test eder.
 *
 * Test kapsamı:
 *  - POST → 201 + DB + Kafka CREATED event (key = ROUTE:{origin}-{dest})
 *  - POST aynı origin-destination → 409 (duplicate route)
 *  - POST origin == destination → 400 (business rule)
 *  - DELETE → 204 + DB'den silindi + Kafka DELETED event
 *  - by-codes query → 200
 *  - BI_SPECIALIST yazma → 403
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
class RouteIntegrationTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";
    private static final String TOPIC = "reference.events";


    @Value("${spring.kafka.bootstrap-servers}")
    String kafkaBootstrapServers;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RouteRepository routeRepository;

    @Autowired
    StationRepository stationRepository;

    // Test'te kullanılacak station ID'leri
    private Long ltbaId;
    private Long ltfmId;
    private Long esenbId;

    private BlockingQueue<ConsumerRecord<String, String>> records;
    private KafkaMessageListenerContainer<String, String> container;

    @BeforeEach
    void setUp() {
        routeRepository.deleteAll();
        stationRepository.deleteAll();

        // Station fixture'ları oluştur (Route için dependency)
        Station ltba = stationRepository.save(
                Station.builder().icaoCode("LTBA").name("Istanbul Ataturk").build());
        Station ltfm = stationRepository.save(
                Station.builder().icaoCode("LTFM").name("Istanbul Havalimani").build());
        Station esenb = stationRepository.save(
                Station.builder().icaoCode("LTAC").name("Esenboga").build());

        ltbaId = ltba.getId();
        ltfmId = ltfm.getId();
        esenbId = esenb.getId();

        // Kafka consumer
        records = new LinkedBlockingQueue<>();
        String bootstrapServers = kafkaBootstrapServers;
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "it-route-consumer-" + System.currentTimeMillis(), "true", bootstrapServers);
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
    @DisplayName("Rota olusturma → 201 + DB kaydi + Kafka CREATED event")
    void createRoute_returns201_persistsInDb_publishesKafkaEvent() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(
                Map.of("originStationId", ltbaId, "destinationStationId", ltfmId));

        // when
        mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originStation.icaoCode").value("LTBA"))
                .andExpect(jsonPath("$.destinationStation.icaoCode").value("LTFM"));

        // then — DB
        assertThat(routeRepository.findAll()).hasSize(1);

        // then — Kafka
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka CREATED event bekleniyor").isNotNull();
        assertThat(record.key()).isEqualTo("ROUTE:LTBA-LTFM");
        assertThat(record.value()).contains("CREATED");
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Ayni origin-destination → 409 Conflict (duplicate rota)")
    void createRoute_duplicateOriginDest_returns409() throws Exception {
        // given — ilk rota
        String body = objectMapper.writeValueAsString(
                Map.of("originStationId", ltbaId, "destinationStationId", ltfmId));
        mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // when — aynı rota
        mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        // then — DB'de tek kayıt
        assertThat(routeRepository.findAll()).hasSize(1);
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Kalkis == Varis → 409 (is kurali: ayni istasyon)")
    void createRoute_sameOriginAndDestination_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("originStationId", ltbaId, "destinationStationId", ltbaId));

        mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict()); // BusinessException → 409
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Rota silme → 204 + DB'den silindi + Kafka DELETED event")
    void deleteRoute_returns204_removesFromDb_publishesDeletedEvent() throws Exception {
        // given
        String createResp = mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("originStationId", ltbaId, "destinationStationId", esenbId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();
        records.clear(); // oluşturma event'ini temizle

        // when
        mockMvc.perform(delete("/api/reference/routes/" + id)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER"))
                .andExpect(status().isNoContent());

        // then — DB
        assertThat(routeRepository.findById(id)).isEmpty();

        // then — Kafka
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka DELETED event bekleniyor").isNotNull();
        assertThat(record.key()).isEqualTo("ROUTE:LTBA-LTAC");
        assertThat(record.value()).contains("DELETED");
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("ICAO kodlarina gore rota arama — GET /by-codes")
    void findByCodes_returnsRoute() throws Exception {
        // given
        mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("originStationId", ltfmId, "destinationStationId", esenbId))))
                .andExpect(status().isCreated());

        // when / then
        mockMvc.perform(get("/api/reference/routes/by-codes")
                        .param("origin", "LTFM")
                        .param("destination", "LTAC")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originStation.icaoCode").value("LTFM"))
                .andExpect(jsonPath("$.destinationStation.icaoCode").value("LTAC"));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "BI_SPECIALIST")
    @DisplayName("BI_SPECIALIST ile rota olusturma → 403")
    void createRoute_withBiSpecialist_returns403() throws Exception {
        mockMvc.perform(post("/api/reference/routes")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "analyst")
                        .header("X-User-Roles", "ROLE_BI_SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("originStationId", ltbaId, "destinationStationId", ltfmId))))
                .andExpect(status().isForbidden());
    }
}
