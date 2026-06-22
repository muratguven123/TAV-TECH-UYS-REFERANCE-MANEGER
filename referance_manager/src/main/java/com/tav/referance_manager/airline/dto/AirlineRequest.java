package com.tav.referance_manager.airline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AirlineRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "IATA kodu 2 büyük harf olmalı") String code
) {}
