package com.tav.referance_manager.airline.mapper;

import com.tav.referance_manager.airline.domain.Airline;
import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AirlineMapper {
    Airline toEntity(AirlineRequest request);
    AirlineResponse toResponse(Airline airline);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AirlineRequest request, @MappingTarget Airline airline);
}
