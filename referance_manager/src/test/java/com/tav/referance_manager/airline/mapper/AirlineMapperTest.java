package com.tav.referance_manager.airline.mapper;

import com.tav.referance_manager.airline.domain.Airline;
import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class AirlineMapperTest {

    private final AirlineMapper mapper = Mappers.getMapper(AirlineMapper.class);

    @Test
    @DisplayName("toEntity → AirlineRequest alanları kopyalanır")
    void toEntity_mapsAllFields() {
        Airline entity = mapper.toEntity(new AirlineRequest("Turkish Airlines", "TK"));
        assertThat(entity.getName()).isEqualTo("Turkish Airlines");
        assertThat(entity.getCode()).isEqualTo("TK");
    }

    @Test
    @DisplayName("toResponse → tüm Airline alanları kopyalanır")
    void toResponse_mapsAllFields() {
        Airline entity = Airline.builder().name("Turkish Airlines").code("TK").build();
        entity.setId(42L);

        AirlineResponse response = mapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Turkish Airlines");
        assertThat(response.code()).isEqualTo("TK");
    }

    @Test
    @DisplayName("updateEntity → null olmayan alanlar overwrite eder")
    void updateEntity_overwritesNonNullFields() {
        Airline entity = Airline.builder().name("Old").code("XY").build();
        mapper.updateEntity(new AirlineRequest("New Name", "TK"), entity);
        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getCode()).isEqualTo("TK");
    }

    @Test
    @DisplayName("toEntity null → null döner")
    void toEntity_null_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("toResponse null → null döner")
    void toResponse_null_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
