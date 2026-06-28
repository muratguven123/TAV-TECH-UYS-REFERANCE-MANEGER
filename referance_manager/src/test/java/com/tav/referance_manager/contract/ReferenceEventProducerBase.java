package com.tav.referance_manager.contract;

import com.tav.referance_manager.airline.dto.AirlineResponse;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import com.tav.uys.events.ChangeType;
import com.tav.uys.events.ReferenceChangedEvent;
import com.tav.uys.events.ReferenceEntityType;
import com.tav.referance_manager.events.ReferenceEventKafkaRelay;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

/**
 * reference.events producer kontratları için SCC base class.
 *
 * <p>SCC tarafından oluşturulan test sınıfları bu sınıfı extend eder.
 * Her trigger metodu {@link ReferenceEventKafkaRelay}'i doğrudan çağırarak
 * {@code reference.events} topic'ine mesaj gönderir.
 *
 * <p><b>Teknik Borç:</b> {@code ReferenceChangedEvent} hem bu modülde hem
 * kullanılmalı.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:rm-contract-db;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "app.gateway.auth.enabled=false",
        // Tip header'ı kapalı — consumer kendi tip eşlemesini yönetir
        "spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
@AutoConfigureMessageVerifier
@EmbeddedKafka(
        topics = {"reference.events"},
        partitions = 1,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(locations = "classpath:/rm-contract-test.properties")
@org.springframework.context.annotation.Import(ReferenceEventProducerBase.ContractVerifierBridgeConfig.class)
public abstract class ReferenceEventProducerBase {

    @Autowired
    private ReferenceEventKafkaRelay relay;

    @BeforeEach
    public void setup() {
        // no-op
    }

    // ─────────────────────────────────────────────────────────────
    // AIRLINE
    // ─────────────────────────────────────────────────────────────

    public void triggerAirlineCreated() {
        AirlineResponse payload = new AirlineResponse(1L, "Turkish Airlines", "TK");
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.AIRLINE, ChangeType.CREATED, "TK", payload));
    }

    public void triggerAirlineDeleted() {
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.AIRLINE, ChangeType.DELETED, "TK", null));
    }

    // ─────────────────────────────────────────────────────────────
    // STATION
    // ─────────────────────────────────────────────────────────────

    public void triggerStationCreated() {
        StationResponse payload = new StationResponse(1L, "LTFM", "Istanbul Airport");
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.STATION, ChangeType.CREATED, "LTFM", payload));
    }

    public void triggerStationUpdated() {
        StationResponse payload = new StationResponse(1L, "LTFM", "Istanbul Airport Revised");
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.STATION, ChangeType.UPDATED, "LTFM", payload));
    }

    // ─────────────────────────────────────────────────────────────
    // AIRCRAFT
    // ─────────────────────────────────────────────────────────────

    public void triggerAircraftCreated() {
        AircraftResponse payload = new AircraftResponse(1L, "B737", "TC-JFA");
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.AIRCRAFT, ChangeType.CREATED, "TC-JFA", payload));
    }

    // ─────────────────────────────────────────────────────────────
    // ROUTE
    // ─────────────────────────────────────────────────────────────

    public void triggerRouteCreated() {
        StationResponse origin = new StationResponse(1L, "LTFM", "Istanbul Airport");
        StationResponse destination = new StationResponse(2L, "LTAC", "Esenboga Airport");
        RouteResponse payload = new RouteResponse(1L, origin, destination);
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.ROUTE, ChangeType.CREATED, "LTFM-LTAC", payload));
    }

    public void triggerRouteDeleted() {
        relay.onReferenceChanged(new ReferenceChangedEvent(
                ReferenceEntityType.ROUTE, ChangeType.DELETED, "LTFM-LTAC", null));
    }

    @org.springframework.boot.test.context.TestConfiguration
    public static class ContractVerifierBridgeConfig {

        @org.springframework.context.annotation.Bean("reference.events")
        public org.springframework.messaging.MessageChannel referenceEventsChannel() {
            return new org.springframework.integration.channel.QueueChannel();
        }

        @org.springframework.kafka.annotation.KafkaListener(
                topics = "reference.events",
                groupId = "contract-verifier-bridge"
        )
        public void bridgeToChannel(
                org.springframework.messaging.Message<com.tav.uys.events.ReferenceChangedEvent> kafkaMessage
        ) {
            org.springframework.messaging.MessageChannel channel = referenceEventsChannel();
            String key = (String) kafkaMessage.getHeaders().get(org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY);
            com.tav.uys.events.ReferenceChangedEvent payload = kafkaMessage.getPayload();
            
            java.util.Map<String, Object> headers = new java.util.HashMap<>();
            headers.put("kafka_messageKey", key);
            
            org.springframework.messaging.Message<?> channelMessage = 
                    org.springframework.messaging.support.MessageBuilder
                            .withPayload(payload)
                            .copyHeaders(headers)
                            .build();
            channel.send(channelMessage);
        }
    }
}
