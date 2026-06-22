package com.tav.referance_manager.station.dto;

import com.tav.referance_manager.common.validation.ValidIcao;
import jakarta.validation.constraints.NotBlank;

public record StationRequest(
        @NotBlank @ValidIcao String icaoCode,
        @NotBlank String name
) {}
