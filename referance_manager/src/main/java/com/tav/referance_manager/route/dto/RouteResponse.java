package com.tav.referance_manager.route.dto;

import com.tav.referance_manager.station.dto.StationResponse;

public record RouteResponse(Long id, StationResponse originStation, StationResponse destinationStation) {}
