package com.tav.referance_manager.aircraft.mapper;

import com.tav.referance_manager.aircraft.domain.Aircraft;
import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AircraftMapper {
    Aircraft toEntity(AircraftRequest request);
    AircraftResponse toResponse(Aircraft aircraft);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AircraftRequest request, @MappingTarget Aircraft aircraft);
}
