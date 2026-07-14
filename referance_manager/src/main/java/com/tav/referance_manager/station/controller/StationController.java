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
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public List<StationResponse> getAll() {
        return stationService.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public StationResponse getById(@PathVariable Long id) {
        return stationService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public ResponseEntity<StationResponse> create(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public StationResponse update(@PathVariable Long id, @Valid @RequestBody StationRequest request) {
        return stationService.update(id, request);
    }

    @GetMapping("/by-code/{icao}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'BI_SPECIALIST', 'ADMIN')")
    public ResponseEntity<StationResponse> findByIcaoCode(@PathVariable String icao) {
        return stationService.findByIcaoCode(icao)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATION_OFFICER', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
