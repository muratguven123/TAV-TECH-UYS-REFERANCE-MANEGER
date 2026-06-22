package com.tav.referance_manager.station.mapper;

import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StationMapper {
    Station toEntity(StationRequest request);
    StationResponse toResponse(Station station);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(StationRequest request, @MappingTarget Station station);
}
