package com.tav.referance_manager.route.repository;

import com.tav.referance_manager.route.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByOriginStationIdAndDestinationStationId(Long originId, Long destinationId);
    Optional<Route> findByOriginStation_IcaoCodeAndDestinationStation_IcaoCode(String originIcao, String destinationIcao);
}
