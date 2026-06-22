package com.tav.referance_manager.station.repository;

import com.tav.referance_manager.station.domain.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByIcaoCode(String icaoCode);
    boolean existsByIcaoCode(String icaoCode);
}
