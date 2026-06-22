package com.tav.referance_manager.station.mapper;

import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-22T10:23:08+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Microsoft)"
)
@Component
public class StationMapperImpl implements StationMapper {

    @Override
    public Station toEntity(StationRequest request) {
        if ( request == null ) {
            return null;
        }

        Station station = new Station();

        station.setIcaoCode( request.icaoCode() );
        station.setName( request.name() );

        return station;
    }

    @Override
    public StationResponse toResponse(Station station) {
        if ( station == null ) {
            return null;
        }

        Long id = null;
        String icaoCode = null;
        String name = null;

        id = station.getId();
        icaoCode = station.getIcaoCode();
        name = station.getName();

        StationResponse stationResponse = new StationResponse( id, icaoCode, name );

        return stationResponse;
    }

    @Override
    public void updateEntity(StationRequest request, Station station) {
        if ( request == null ) {
            return;
        }

        if ( request.icaoCode() != null ) {
            station.setIcaoCode( request.icaoCode() );
        }
        if ( request.name() != null ) {
            station.setName( request.name() );
        }
    }
}
