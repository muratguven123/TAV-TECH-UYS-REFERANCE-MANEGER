package com.tav.referance_manager.aircraft.domain;

import com.tav.referance_manager.common.BaseEntity;
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
@Table(name = "aircrafts",
       uniqueConstraints = @UniqueConstraint(name = "uk_aircraft_tail", columnNames = "tail_number"))
public class Aircraft extends BaseEntity {

    @NotNull
    @Column(nullable = false)
    private String type;

    @NotNull
    @Column(name = "tail_number", nullable = false)
    private String tailNumber;
}
