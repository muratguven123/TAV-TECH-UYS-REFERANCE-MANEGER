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
    public List<AirlineResponse> getAll() {
        return airlineService.getAll();
    }

    @GetMapping("/{id}")
    public AirlineResponse getById(@PathVariable Long id) {
        return airlineService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<AirlineResponse> create(@Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public AirlineResponse update(@PathVariable Long id, @Valid @RequestBody AirlineRequest request) {
        return airlineService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        airlineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
