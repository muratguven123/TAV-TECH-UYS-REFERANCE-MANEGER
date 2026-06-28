package com.tav.referance_manager.station.service;

import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.events.ReferenceEventPublisher;
import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import com.tav.referance_manager.station.mapper.StationMapper;
import com.tav.referance_manager.station.repository.StationRepository;
import com.tav.uys.events.ChangeType;
import com.tav.uys.events.ReferenceEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final StationMapper stationMapper;
    private final ReferenceEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<StationResponse> getAll() {
        return stationRepository.findAll().stream().map(stationMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StationResponse getById(Long id) {
        return stationMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public StationResponse create(StationRequest request) {
        if (stationRepository.existsByIcaoCode(request.icaoCode())) {
            throw new BusinessException("Bu ICAO kodu zaten mevcut: " + request.icaoCode());
        }
        Station saved = stationRepository.save(stationMapper.toEntity(request));
        StationResponse response = stationMapper.toResponse(saved);
        eventPublisher.publish(ReferenceEntityType.STATION, ChangeType.CREATED, saved.getIcaoCode(), response);
        return response;
    }

    @Transactional
    public StationResponse update(Long id, StationRequest request) {
        Station station = findOrThrow(id);
        if (!station.getIcaoCode().equals(request.icaoCode()) &&
                stationRepository.existsByIcaoCode(request.icaoCode())) {
            throw new BusinessException("Bu ICAO kodu zaten mevcut: " + request.icaoCode());
        }
        stationMapper.updateEntity(request, station);
        StationResponse response = stationMapper.toResponse(stationRepository.save(station));
        eventPublisher.publish(ReferenceEntityType.STATION, ChangeType.UPDATED, station.getIcaoCode(), response);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        Station station = findOrThrow(id);
        String icaoCode = station.getIcaoCode();
        stationRepository.delete(station);
        eventPublisher.publish(ReferenceEntityType.STATION, ChangeType.DELETED, icaoCode, null);
    }

    @Transactional(readOnly = true)
    public Optional<StationResponse> findByIcaoCode(String icao) {
        return stationRepository.findByIcaoCode(icao).map(stationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Station findEntityById(Long id) {
        return findOrThrow(id);
    }

    private Station findOrThrow(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("İstasyon bulunamadı: " + id));
    }
}
