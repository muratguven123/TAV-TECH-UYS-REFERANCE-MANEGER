package com.tav.referance_manager.aircraft.controller;

import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import com.tav.referance_manager.aircraft.service.AircraftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reference/aircrafts")
@RequiredArgsConstructor
public class AircraftController {

    private final AircraftService aircraftService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public List<AircraftResponse> getAll() {
        return aircraftService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public AircraftResponse getById(@PathVariable Long id) {
        return aircraftService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public ResponseEntity<AircraftResponse> create(@Valid @RequestBody AircraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aircraftService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public AircraftResponse update(@PathVariable Long id, @Valid @RequestBody AircraftRequest request) {
        return aircraftService.update(id, request);
    }

    @GetMapping("/by-tail/{tailNumber}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public ResponseEntity<AircraftResponse> findByTailNumber(@PathVariable String tailNumber) {
        return aircraftService.findByTailNumber(tailNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aircraftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
