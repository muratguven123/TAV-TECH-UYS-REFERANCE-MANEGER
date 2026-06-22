package com.tav.referance_manager.route.repository;

import com.tav.referance_manager.route.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByOriginStationIdAndDestinationStationId(Long originId, Long destinationId);
}
