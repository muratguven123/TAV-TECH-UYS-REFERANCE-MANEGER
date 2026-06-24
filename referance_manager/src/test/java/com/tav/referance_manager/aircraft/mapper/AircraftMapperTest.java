package com.tav.referance_manager.aircraft.mapper;

import com.tav.referance_manager.aircraft.domain.Aircraft;
import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class AircraftMapperTest {

    private final AircraftMapper mapper = Mappers.getMapper(AircraftMapper.class);

    @Test
    @DisplayName("toEntity → tüm alanlar kopyalanır")
    void toEntity_mapsAllFields() {
        Aircraft entity = mapper.toEntity(new AircraftRequest("A320", "TC-JFA"));
        assertThat(entity.getType()).isEqualTo("A320");
        assertThat(entity.getTailNumber()).isEqualTo("TC-JFA");
    }

    @Test
    @DisplayName("toResponse → tüm alanlar kopyalanır")
    void toResponse_mapsAllFields() {
        Aircraft entity = Aircraft.builder().type("A320").tailNumber("TC-JFA").build();
        entity.setId(7L);
        AircraftResponse response = mapper.toResponse(entity);
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.type()).isEqualTo("A320");
        assertThat(response.tailNumber()).isEqualTo("TC-JFA");
    }

    @Test
    @DisplayName("updateEntity → non-null alanlar overwrite")
    void updateEntity_overwritesNonNullFields() {
        Aircraft entity = Aircraft.builder().type("A319").tailNumber("TC-JFA").build();
        mapper.updateEntity(new AircraftRequest("A320", "TC-JFB"), entity);
        assertThat(entity.getType()).isEqualTo("A320");
        assertThat(entity.getTailNumber()).isEqualTo("TC-JFB");
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
