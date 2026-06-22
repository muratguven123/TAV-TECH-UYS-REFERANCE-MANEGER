package com.tav.referance_manager.airline.service;

import com.tav.referance_manager.airline.domain.Airline;
import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import com.tav.referance_manager.airline.mapper.AirlineMapper;
import com.tav.referance_manager.airline.repository.AirlineRepository;
import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.events.ReferenceEventPublisher;
import com.tav.referance_manager.events.ChangeType;
import com.tav.referance_manager.events.ReferenceEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMapper airlineMapper;
    private final ReferenceEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AirlineResponse> getAll() {
        return airlineRepository.findAll().stream().map(airlineMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AirlineResponse getById(Long id) {
        return airlineMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public AirlineResponse create(AirlineRequest request) {
        if (airlineRepository.existsByCode(request.code())) {
            throw new BusinessException("Bu IATA kodu zaten mevcut: " + request.code());
        }
        Airline saved = airlineRepository.save(airlineMapper.toEntity(request));
        AirlineResponse response = airlineMapper.toResponse(saved);
        eventPublisher.publish(ReferenceEntityType.AIRLINE, ChangeType.CREATED, saved.getCode(), response);
        return response;
    }

    @Transactional
    public AirlineResponse update(Long id, AirlineRequest request) {
        Airline airline = findOrThrow(id);
        if (!airline.getCode().equals(request.code()) && airlineRepository.existsByCode(request.code())) {
            throw new BusinessException("Bu IATA kodu zaten mevcut: " + request.code());
        }
        airlineMapper.updateEntity(request, airline);
        AirlineResponse response = airlineMapper.toResponse(airlineRepository.save(airline));
        eventPublisher.publish(ReferenceEntityType.AIRLINE, ChangeType.UPDATED, airline.getCode(), response);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Airline airline = findOrThrow(id);
        String code = airline.getCode();
        airlineRepository.delete(airline);
        eventPublisher.publish(ReferenceEntityType.AIRLINE, ChangeType.DELETED, code, null);
    }

    private Airline findOrThrow(Long id) {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Havayolu bulunamadı: " + id));
    }
}
