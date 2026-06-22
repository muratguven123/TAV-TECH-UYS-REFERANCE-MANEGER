package com.tav.referance_manager.route.mapper;

import com.tav.referance_manager.route.domain.Route;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.station.dto.StationResponse;
import com.tav.referance_manager.station.mapper.StationMapper;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-22T10:23:08+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Microsoft)"
)
@Component
public class RouteMapperImpl implements RouteMapper {

    @Autowired
    private StationMapper stationMapper;

    @Override
    public RouteResponse toResponse(Route route) {
        if ( route == null ) {
            return null;
        }

        Long id = null;
        StationResponse originStation = null;
        StationResponse destinationStation = null;

        id = route.getId();
        originStation = stationMapper.toResponse( route.getOriginStation() );
        destinationStation = stationMapper.toResponse( route.getDestinationStation() );

        RouteResponse routeResponse = new RouteResponse( id, originStation, destinationStation );

        return routeResponse;
    }
}
