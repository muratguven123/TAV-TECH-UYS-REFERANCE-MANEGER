package com.tav.referance_manager.route.mapper;

import com.tav.referance_manager.route.domain.Route;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.station.mapper.StationMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = StationMapper.class)
public interface RouteMapper {
    RouteResponse toResponse(Route route);
}
