package com.tav.referance_manager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.aircraft.repository.AircraftRepository;
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
 * Aircraft CRUD + Kafka event emission integration testleri.
 *
 * Gerçek MySQL Testcontainer + gerçek Kafka Testcontainer kullanılır.
 * FAS ContainersConfig ve ReferenceStationIntegrationTest pattern'ini referans alır.
 *
 * Test kapsamı:
 *  - POST /api/reference/aircrafts → 201 + DB'de kayıt + Kafka event
 *  - POST duplicate tailNumber → 409
 *  - DELETE → 204 + DB'den silindi + DELETED event Kafka'da
 *  - PUT update → 200 + UPDATED event Kafka'da
 *  - OPERATION_OFFICER dışı rol → 403
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
class AircraftIntegrationTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";
    private static final String TOPIC = "reference.events";


    @Value("${spring.kafka.bootstrap-servers}")
    String kafkaBootstrapServers;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AircraftRepository aircraftRepository;

    // Kafka test consumer
    private BlockingQueue<ConsumerRecord<String, String>> records;
    private KafkaMessageListenerContainer<String, String> container;

    @BeforeEach
    void setUp() {
        aircraftRepository.deleteAll();
        records = new LinkedBlockingQueue<>();

        // Kafka bootstrap bilgisi @ServiceConnection ile otomatik set edilir
        // Ancak KafkaTestUtils'in EmbeddedKafkaBroker'ı yok; spring.kafka.bootstrap-servers'ı kullan
        String bootstrapServers = kafkaBootstrapServers;

        Map<String, Object> props = KafkaTestUtils.consumerProps(
                "it-aircraft-consumer-" + System.currentTimeMillis(), "true", bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ContainerProperties containerProps = new ContainerProperties(TOPIC);
        containerProps.setMessageListener((MessageListener<String, String>) records::add);

        container = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(props), containerProps);
        container.start();

        // Container'ın topic'e assign olmasını bekle
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
    @DisplayName("Ucak olusturma → 201 + DB kaydi + Kafka CREATED event")
    void createAircraft_returns201_persistsInDb_publishesKafkaEvent() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(
                Map.of("type", "B738", "tailNumber", "TC-JFA"));

        // when
        mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tailNumber").value("TC-JFA"))
                .andExpect(jsonPath("$.type").value("B738"));

        // then — DB
        assertThat(aircraftRepository.findByTailNumber("TC-JFA")).isPresent();

        // then — Kafka event
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka'dan CREATED event bekleniyor").isNotNull();
        assertThat(record.key()).isEqualTo("AIRCRAFT:TC-JFA");
        assertThat(record.value()).contains("CREATED").contains("TC-JFA");
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Duplicate tailNumber → 409 Conflict")
    void createAircraft_duplicateTailNumber_returns409() throws Exception {
        // given — ilk uçak
        mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "A320", "tailNumber", "TC-DUP"))))
                .andExpect(status().isCreated());

        // when — aynı tailNumber
        mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "B777", "tailNumber", "TC-DUP"))))
                .andExpect(status().isConflict());

        // then — DB'de tek kayıt
        assertThat(aircraftRepository.findAll()).hasSize(1);
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Ucak silme → 204 + DB'den silindi + Kafka DELETED event")
    void deleteAircraft_returns204_removesFromDb_publishesDeletedEvent() throws Exception {
        // given — önce oluştur
        String createResp = mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "B738", "tailNumber", "TC-DEL"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();
        records.clear(); // oluşturma event'ini temizle

        // when — sil
        mockMvc.perform(delete("/api/reference/aircrafts/" + id)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER"))
                .andExpect(status().isNoContent());

        // then — DB'den silindi
        assertThat(aircraftRepository.findById(id)).isEmpty();

        // then — Kafka'da DELETED event
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka'dan DELETED event bekleniyor").isNotNull();
        assertThat(record.key()).isEqualTo("AIRCRAFT:TC-DEL");
        assertThat(record.value()).contains("DELETED");
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Ucak guncelleme → 200 + Kafka UPDATED event")
    void updateAircraft_returns200_publishesUpdatedEvent() throws Exception {
        // given
        String createResp = mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "A319", "tailNumber", "TC-UPD"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResp).get("id").asLong();
        records.clear();

        // when
        mockMvc.perform(put("/api/reference/aircrafts/" + id)
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "A320", "tailNumber", "TC-UPD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("A320"));

        // then — Kafka
        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).as("Kafka'dan UPDATED event bekleniyor").isNotNull();
        assertThat(record.value()).contains("UPDATED");
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "BI_SPECIALIST")
    @DisplayName("BI_SPECIALIST rolu ile POST → 403 Forbidden")
    void createAircraft_withBiSpecialist_returns403() throws Exception {
        mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "analyst")
                        .header("X-User-Roles", "ROLE_BI_SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "B738", "tailNumber", "TC-DENY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "OPERATION_OFFICER")
    @DisplayName("Tail numarasina gore arama — GET /by-tail/{tailNumber}")
    void findByTailNumber_returnsAircraft() throws Exception {
        // given
        mockMvc.perform(post("/api/reference/aircrafts")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("type", "B777", "tailNumber", "TC-FIND"))))
                .andExpect(status().isCreated());

        // when / then
        mockMvc.perform(get("/api/reference/aircrafts/by-tail/TC-FIND")
                        .header("X-Gateway-Secret", GATEWAY_SECRET)
                        .header("X-User-Name", "operator")
                        .header("X-User-Roles", "ROLE_OPERATION_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tailNumber").value("TC-FIND"));
    }
}
