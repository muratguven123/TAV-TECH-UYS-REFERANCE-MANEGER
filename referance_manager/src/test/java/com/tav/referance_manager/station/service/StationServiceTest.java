package com.tav.referance_manager.station.service;

import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.events.ChangeType;
import com.tav.referance_manager.events.ReferenceEntityType;
import com.tav.referance_manager.events.ReferenceEventPublisher;
import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import com.tav.referance_manager.station.mapper.StationMapper;
import com.tav.referance_manager.station.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock StationRepository stationRepository;
    @Mock StationMapper stationMapper;
    @Mock ReferenceEventPublisher eventPublisher;

    @InjectMocks
    StationService stationService;

    private Station station;
    private StationResponse response;
    private StationRequest request;

    @BeforeEach
    void setUp() {
        station = Station.builder().icaoCode("LTFM").name("İstanbul Havalimanı").build();
        station.setId(1L);
        response = new StationResponse(1L, "LTFM", "İstanbul Havalimanı");
        request = new StationRequest("LTFM", "İstanbul Havalimanı");
    }

    @Test
    @DisplayName("create: başarılı yolda kaydeder ve event yayınlar")
    void create_happyPath_savesAndPublishesEvent() {
        // given
        when(stationRepository.existsByIcaoCode("LTFM")).thenReturn(false);
        when(stationMapper.toEntity(request)).thenReturn(station);
        when(stationRepository.save(station)).thenReturn(station);
        when(stationMapper.toResponse(station)).thenReturn(response);

        // when
        StationResponse result = stationService.create(request);

        // then
        assertThat(result).isEqualTo(response);
        verify(eventPublisher).publish(ReferenceEntityType.STATION, ChangeType.CREATED, "LTFM", response);
    }

    @Test
    @DisplayName("create: tekrar eden ICAO kodu varsa BusinessException fırlatılır")
    void create_whenDuplicateIcao_throwsException() {
        // given
        when(stationRepository.existsByIcaoCode("LTFM")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> stationService.create(request))
                .isInstanceOf(BusinessException.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll: tüm istasyonları döner")
    void findAll_returnsAllStations() {
        // given
        when(stationRepository.findAll()).thenReturn(List.of(station));
        when(stationMapper.toResponse(station)).thenReturn(response);

        // when
        List<StationResponse> result = stationService.getAll();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findById: kayıt yoksa NotFoundException fırlatılır")
    void findById_whenNotFound_throwsNotFoundException() {
        // given
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> stationService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update: başarılı yolda günceller ve event yayınlar")
    void update_happyPath_updatesAndPublishes() {
        // given
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(station)).thenReturn(station);
        when(stationMapper.toResponse(station)).thenReturn(response);

        // when
        StationResponse result = stationService.update(1L, request);

        // then
        verify(stationMapper).updateEntity(request, station);
        verify(eventPublisher).publish(eq(ReferenceEntityType.STATION), eq(ChangeType.UPDATED), any(), any());
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("delete: başarılı yolda siler ve event yayınlar")
    void delete_happyPath_deletesAndPublishes() {
        // given
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        // when
        stationService.delete(1L);

        // then
        verify(stationRepository).delete(station);
        verify(eventPublisher).publish(ReferenceEntityType.STATION, ChangeType.DELETED, "LTFM", null);
    }
}
