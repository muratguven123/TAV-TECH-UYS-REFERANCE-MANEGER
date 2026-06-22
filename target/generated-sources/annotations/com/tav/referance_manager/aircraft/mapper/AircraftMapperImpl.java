package com.tav.referance_manager.aircraft.mapper;

import com.tav.referance_manager.aircraft.domain.Aircraft;
import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-22T10:23:08+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Microsoft)"
)
@Component
public class AircraftMapperImpl implements AircraftMapper {

    @Override
    public Aircraft toEntity(AircraftRequest request) {
        if ( request == null ) {
            return null;
        }

        Aircraft aircraft = new Aircraft();

        aircraft.setType( request.type() );
        aircraft.setTailNumber( request.tailNumber() );

        return aircraft;
    }

    @Override
    public AircraftResponse toResponse(Aircraft aircraft) {
        if ( aircraft == null ) {
            return null;
        }

        Long id = null;
        String type = null;
        String tailNumber = null;

        id = aircraft.getId();
        type = aircraft.getType();
        tailNumber = aircraft.getTailNumber();

        AircraftResponse aircraftResponse = new AircraftResponse( id, type, tailNumber );

        return aircraftResponse;
    }

    @Override
    public void updateEntity(AircraftRequest request, Aircraft aircraft) {
        if ( request == null ) {
            return;
        }

        if ( request.type() != null ) {
            aircraft.setType( request.type() );
        }
        if ( request.tailNumber() != null ) {
            aircraft.setTailNumber( request.tailNumber() );
        }
    }
}
