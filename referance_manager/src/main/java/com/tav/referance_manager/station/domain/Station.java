package com.tav.referance_manager.station.domain;

import com.tav.referance_manager.common.BaseEntity;
import com.tav.referance_manager.common.validation.ValidIcao;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "stations",
       uniqueConstraints = @UniqueConstraint(name = "uk_station_icao", columnNames = "icao_code"))
public class Station extends BaseEntity {

    @NotNull
    @ValidIcao
    @Column(name = "icao_code", nullable = false, length = 4)
    private String icaoCode;

    @NotNull
    @Column(nullable = false)
    private String name;
}
