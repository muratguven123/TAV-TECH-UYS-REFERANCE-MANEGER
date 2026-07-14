package com.tav.referance_manager.airline.controller;

import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import com.tav.referance_manager.airline.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reference/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public List<AirlineResponse> getAll() {
        return airlineService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public AirlineResponse getById(@PathVariable Long id) {
        return airlineService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public ResponseEntity<AirlineResponse> create(@Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public AirlineResponse update(@PathVariable Long id, @Valid @RequestBody AirlineRequest request) {
        return airlineService.update(id, request);
    }

    @GetMapping("/by-code/{iata}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public ResponseEntity<AirlineResponse> findByIataCode(@PathVariable String iata) {
        return airlineService.findByIataCode(iata)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        airlineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
