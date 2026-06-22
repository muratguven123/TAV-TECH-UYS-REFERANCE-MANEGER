package com.tav.referance_manager.airline.domain;

import com.tav.referance_manager.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "airlines",
       uniqueConstraints = @UniqueConstraint(name = "uk_airline_code", columnNames = "code"))
public class Airline extends BaseEntity {

    @NotNull
    @Column(nullable = false)
    private String name;

    @NotNull
    @Pattern(regexp = "^[A-Z]{2}$", message = "IATA kodu 2 büyük harf olmalı")
    @Column(nullable = false, length = 2)
    private String code;
}
