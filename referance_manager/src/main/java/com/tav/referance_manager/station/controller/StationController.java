package com.tav.referance_manager.station.controller;

import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import com.tav.referance_manager.station.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reference/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    public List<StationResponse> getAll() {
        return stationService.getAll();
    }

    @GetMapping("/{id}")
    public StationResponse getById(@PathVariable Long id) {
        return stationService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<StationResponse> create(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public StationResponse update(@PathVariable Long id, @Valid @RequestBody StationRequest request) {
        return stationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
