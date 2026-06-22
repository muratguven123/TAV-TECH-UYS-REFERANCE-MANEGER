package com.tav.referance_manager.aircraft.repository;

import com.tav.referance_manager.aircraft.domain.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByTailNumber(String tailNumber);
    boolean existsByTailNumber(String tailNumber);
}
