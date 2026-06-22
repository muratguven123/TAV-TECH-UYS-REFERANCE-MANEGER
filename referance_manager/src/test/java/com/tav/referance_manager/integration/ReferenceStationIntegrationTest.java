package com.tav.referance_manager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.station.dto.StationRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
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
class ReferenceStationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private BlockingQueue<ConsumerRecord<String, String>> records;
    private KafkaMessageListenerContainer<String, String> container;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ContainerProperties containerProps = new ContainerProperties("reference.events");
        containerProps.setMessageListener((MessageListener<String, String>) records::add);

        container = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(consumerProps), containerProps);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @Test
    @WithMockUser(roles = "OPERATION_OFFICER")
    void createStation_shouldReturn201AndPublishKafkaEvent() throws Exception {
        StationRequest request = new StationRequest("LTBA", "İstanbul Atatürk");

        mockMvc.perform(post("/api/reference/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.icaoCode").value("LTBA"));

        ConsumerRecord<String, String> record = records.poll(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("STATION:LTBA");
        assertThat(record.value()).contains("CREATED").contains("LTBA");
    }

    @Test
    @WithMockUser(roles = "BI_SPECIALIST")
    void createStation_withBiSpecialist_shouldReturn403() throws Exception {
        StationRequest request = new StationRequest("LTFM", "İstanbul Yeni Havalimanı");

        mockMvc.perform(post("/api/reference/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATION_OFFICER")
    void createStation_withInvalidIcao_shouldReturn400() throws Exception {
        StationRequest request = new StationRequest("LTB1", "Geçersiz İstasyon");

        mockMvc.perform(post("/api/reference/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.icaoCode").exists());
    }
}
