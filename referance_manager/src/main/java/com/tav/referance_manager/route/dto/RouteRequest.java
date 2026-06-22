package com.tav.referance_manager.route.dto;

import jakarta.validation.constraints.NotNull;

public record RouteRequest(
        @NotNull Long originStationId,
        @NotNull Long destinationStationId
) {}
