package com.tav.referance_manager.route.controller;

import com.tav.referance_manager.route.dto.RouteRequest;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.route.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reference/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public List<RouteResponse> getAll() {
        return routeService.getAll();
    }

    @GetMapping("/{id}")
    public RouteResponse getById(@PathVariable Long id) {
        return routeService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<RouteResponse> create(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATION_OFFICER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        routeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
