package com.tav.referance_manager.aircraft.dto;

import jakarta.validation.constraints.NotBlank;

public record AircraftRequest(
        @NotBlank String type,
        @NotBlank String tailNumber
) {}
