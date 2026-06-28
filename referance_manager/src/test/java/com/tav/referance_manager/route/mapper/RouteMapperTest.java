package com.tav.referance_manager.route.mapper;

import com.tav.referance_manager.route.domain.Route;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.mapper.StationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RouteMapperTest {

    private StationMapper stationMapper = Mappers.getMapper(StationMapper.class);
    private RouteMapper mapper = Mappers.getMapper(RouteMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mapper, "stationMapper", stationMapper);
    }

    @Test
    @DisplayName("toResponse → Route iki Station alanını da nested olarak kopyalar")
    void toResponse_mapsNestedStations() {
        Station origin = Station.builder().icaoCode("LTFM").name("Istanbul").build();
        origin.setId(10L);
        Station destination = Station.builder().icaoCode("LTAC").name("Ankara").build();
        destination.setId(20L);
        Route route = Route.builder()
                .originStation(origin)
                .destinationStation(destination)
                .build();
        route.setId(1L);

        RouteResponse r = mapper.toResponse(route);

        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.originStation().icaoCode()).isEqualTo("LTFM");
        assertThat(r.originStation().id()).isEqualTo(10L);
        assertThat(r.destinationStation().icaoCode()).isEqualTo("LTAC");
        assertThat(r.destinationStation().id()).isEqualTo(20L);
    }

    @Test
    @DisplayName("toResponse null → null döner")
    void toResponse_null_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}

