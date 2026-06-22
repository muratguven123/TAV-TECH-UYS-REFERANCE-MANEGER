package com.tav.referance_manager.aircraft.service;

import com.tav.referance_manager.aircraft.domain.Aircraft;
import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import com.tav.referance_manager.aircraft.mapper.AircraftMapper;
import com.tav.referance_manager.aircraft.repository.AircraftRepository;
import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.events.ReferenceEventPublisher;
import com.tav.referance_manager.events.ChangeType;
import com.tav.referance_manager.events.ReferenceEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AircraftMapper aircraftMapper;
    private final ReferenceEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AircraftResponse> getAll() {
        return aircraftRepository.findAll().stream().map(aircraftMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AircraftResponse getById(Long id) {
        return aircraftMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public AircraftResponse create(AircraftRequest request) {
        if (aircraftRepository.existsByTailNumber(request.tailNumber())) {
            throw new BusinessException("Bu kuyruk numarası zaten mevcut: " + request.tailNumber());
        }
        Aircraft saved = aircraftRepository.save(aircraftMapper.toEntity(request));
        AircraftResponse response = aircraftMapper.toResponse(saved);
        eventPublisher.publish(ReferenceEntityType.AIRCRAFT, ChangeType.CREATED, saved.getTailNumber(), response);
        return response;
    }

    @Transactional
    public AircraftResponse update(Long id, AircraftRequest request) {
        Aircraft aircraft = findOrThrow(id);
        if (!aircraft.getTailNumber().equals(request.tailNumber()) &&
                aircraftRepository.existsByTailNumber(request.tailNumber())) {
            throw new BusinessException("Bu kuyruk numarası zaten mevcut: " + request.tailNumber());
        }
        aircraftMapper.updateEntity(request, aircraft);
        AircraftResponse response = aircraftMapper.toResponse(aircraftRepository.save(aircraft));
        eventPublisher.publish(ReferenceEntityType.AIRCRAFT, ChangeType.UPDATED, aircraft.getTailNumber(), response);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Aircraft aircraft = findOrThrow(id);
        String tailNumber = aircraft.getTailNumber();
        aircraftRepository.delete(aircraft);
        eventPublisher.publish(ReferenceEntityType.AIRCRAFT, ChangeType.DELETED, tailNumber, null);
    }

    @Transactional(readOnly = true)
    public Optional<AircraftResponse> findByTailNumber(String tailNumber) {
        return aircraftRepository.findByTailNumber(tailNumber).map(aircraftMapper::toResponse);
    }

    private Aircraft findOrThrow(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Uçak bulunamadı: " + id));
    }
}
