package com.tav.referance_manager.station.mapper;

import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class StationMapperTest {

    private final StationMapper mapper = Mappers.getMapper(StationMapper.class);

    @Test
    @DisplayName("toEntity → tüm alanlar kopyalanır")
    void toEntity_mapsAllFields() {
        Station entity = mapper.toEntity(new StationRequest("LTFM", "Istanbul Airport"));
        assertThat(entity.getIcaoCode()).isEqualTo("LTFM");
        assertThat(entity.getName()).isEqualTo("Istanbul Airport");
    }

    @Test
    @DisplayName("toResponse → tüm alanlar kopyalanır")
    void toResponse_mapsAllFields() {
        Station entity = Station.builder().icaoCode("LTFM").name("Istanbul Airport").build();
        entity.setId(3L);
        StationResponse r = mapper.toResponse(entity);
        assertThat(r.id()).isEqualTo(3L);
        assertThat(r.icaoCode()).isEqualTo("LTFM");
        assertThat(r.name()).isEqualTo("Istanbul Airport");
    }

    @Test
    @DisplayName("updateEntity → non-null alanlar overwrite")
    void updateEntity_overwritesNonNullFields() {
        Station entity = Station.builder().icaoCode("LTBA").name("Atatürk").build();
        mapper.updateEntity(new StationRequest("LTFM", "Istanbul"), entity);
        assertThat(entity.getIcaoCode()).isEqualTo("LTFM");
        assertThat(entity.getName()).isEqualTo("Istanbul");
    }

    @Test
    @DisplayName("toEntity null → null")
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toResponse null → null")
    void toResponse_null_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
