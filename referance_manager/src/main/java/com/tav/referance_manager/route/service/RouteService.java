package com.tav.referance_manager.route.service;

import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.events.ReferenceEventPublisher;
import com.tav.referance_manager.route.domain.Route;
import com.tav.referance_manager.route.dto.RouteRequest;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.route.mapper.RouteMapper;
import com.tav.referance_manager.route.repository.RouteRepository;
import com.tav.referance_manager.station.service.StationService;
import com.tav.referance_manager.events.ChangeType;
import com.tav.referance_manager.events.ReferenceEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;
    private final StationService stationService;
    private final ReferenceEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<RouteResponse> getAll() {
        return routeRepository.findAll().stream().map(routeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getById(Long id) {
        return routeMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public RouteResponse create(RouteRequest request) {
        if (request.originStationId().equals(request.destinationStationId())) {
            throw new BusinessException("Kalkış ve varış istasyonu aynı olamaz");
        }
        if (routeRepository.existsByOriginStationIdAndDestinationStationId(
                request.originStationId(), request.destinationStationId())) {
            throw new BusinessException("Bu rota zaten mevcut");
        }
        Route route = Route.builder()
                .originStation(stationService.findEntityById(request.originStationId()))
                .destinationStation(stationService.findEntityById(request.destinationStationId()))
                .build();
        Route saved = routeRepository.save(route);
        RouteResponse response = routeMapper.toResponse(saved);
        String routeKey = buildRouteKey(saved);
        eventPublisher.publish(ReferenceEntityType.ROUTE, ChangeType.CREATED, routeKey, response);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Route route = findOrThrow(id);
        String routeKey = buildRouteKey(route);
        routeRepository.delete(route);
        eventPublisher.publish(ReferenceEntityType.ROUTE, ChangeType.DELETED, routeKey, null);
    }

    @Transactional(readOnly = true)
    public Optional<RouteResponse> findByCodes(String originIcao, String destinationIcao) {
        return routeRepository
                .findByOriginStation_IcaoCodeAndDestinationStation_IcaoCode(originIcao, destinationIcao)
                .map(routeMapper::toResponse);
    }

    private Route findOrThrow(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rota bulunamadı: " + id));
    }

    private String buildRouteKey(Route route) {
        return route.getOriginStation().getIcaoCode() + "-" + route.getDestinationStation().getIcaoCode();
    }
}
