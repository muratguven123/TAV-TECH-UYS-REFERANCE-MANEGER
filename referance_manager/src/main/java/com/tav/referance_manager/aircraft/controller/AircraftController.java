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
    public List<AircraftResponse> getAll() {
        return aircraftService.getAll();
    }

    @GetMapping("/{id}")
    public AircraftResponse getById(@PathVariable Long id) {
        return aircraftService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<AircraftResponse> create(@Valid @RequestBody AircraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aircraftService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public AircraftResponse update(@PathVariable Long id, @Valid @RequestBody AircraftRequest request) {
        return aircraftService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aircraftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
