package com.tav.referance_manager.route.domain;

import com.tav.referance_manager.common.BaseEntity;
import com.tav.referance_manager.station.domain.Station;
import jakarta.persistence.*;
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
@Table(name = "routes",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_route_origin_destination",
               columnNames = {"origin_station_id", "destination_station_id"}))
public class Route extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_station_id", nullable = false)
    private Station originStation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_station_id", nullable = false)
    private Station destinationStation;
}
