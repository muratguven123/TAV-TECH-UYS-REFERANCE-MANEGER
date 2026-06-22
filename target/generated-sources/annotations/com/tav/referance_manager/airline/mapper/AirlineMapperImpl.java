package com.tav.referance_manager.airline.mapper;

import com.tav.referance_manager.airline.domain.Airline;
import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-22T10:23:08+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.11 (Microsoft)"
)
@Component
public class AirlineMapperImpl implements AirlineMapper {

    @Override
    public Airline toEntity(AirlineRequest request) {
        if ( request == null ) {
            return null;
        }

        Airline airline = new Airline();

        airline.setName( request.name() );
        airline.setCode( request.code() );

        return airline;
    }

    @Override
    public AirlineResponse toResponse(Airline airline) {
        if ( airline == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String code = null;

        id = airline.getId();
        name = airline.getName();
        code = airline.getCode();

        AirlineResponse airlineResponse = new AirlineResponse( id, name, code );

        return airlineResponse;
    }

    @Override
    public void updateEntity(AirlineRequest request, Airline airline) {
        if ( request == null ) {
            return;
        }

        if ( request.name() != null ) {
            airline.setName( request.name() );
        }
        if ( request.code() != null ) {
            airline.setCode( request.code() );
        }
    }
}
