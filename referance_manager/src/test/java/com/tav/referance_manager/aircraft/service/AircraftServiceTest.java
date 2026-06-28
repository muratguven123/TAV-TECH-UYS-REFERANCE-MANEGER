package com.tav.referance_manager.aircraft.service;

import com.tav.referance_manager.aircraft.domain.Aircraft;
import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import com.tav.referance_manager.aircraft.mapper.AircraftMapper;
import com.tav.referance_manager.aircraft.repository.AircraftRepository;
import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.uys.events.ChangeType;
import com.tav.uys.events.ReferenceEntityType;
import com.tav.referance_manager.events.ReferenceEventPublisher;
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
class AircraftServiceTest {

    @Mock AircraftRepository aircraftRepository;
    @Mock AircraftMapper aircraftMapper;
    @Mock ReferenceEventPublisher eventPublisher;

    @InjectMocks
    AircraftService aircraftService;

    private Aircraft aircraft;
    private AircraftResponse response;
    private AircraftRequest request;

    @BeforeEach
    void setUp() {
        aircraft = Aircraft.builder().type("B738").tailNumber("TC-JFA").build();
        aircraft.setId(1L);
        response = new AircraftResponse(1L, "B738", "TC-JFA");
        request = new AircraftRequest("B738", "TC-JFA");
    }

    @Test
    @DisplayName("create: başarılı yolda kaydeder ve event yayınlar")
    void create_happyPath_savesAndPublishesEvent() {
        // given
        when(aircraftRepository.existsByTailNumber("TC-JFA")).thenReturn(false);
        when(aircraftMapper.toEntity(request)).thenReturn(aircraft);
        when(aircraftRepository.save(aircraft)).thenReturn(aircraft);
        when(aircraftMapper.toResponse(aircraft)).thenReturn(response);

        // when
        AircraftResponse result = aircraftService.create(request);

        // then
        assertThat(result).isEqualTo(response);
        verify(aircraftRepository).save(aircraft);
        verify(eventPublisher).publish(ReferenceEntityType.AIRCRAFT, ChangeType.CREATED, "TC-JFA", response);
    }

    @Test
    @DisplayName("create: tekrar eden tailNumber varsa BusinessException fırlatılır")
    void create_whenDuplicateTailNumber_throwsException() {
        // given
        when(aircraftRepository.existsByTailNumber("TC-JFA")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> aircraftService.create(request))
                .isInstanceOf(BusinessException.class);
        verify(aircraftRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll: tüm uçakları döner")
    void findAll_returnsAllAircraft() {
        // given
        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft));
        when(aircraftMapper.toResponse(aircraft)).thenReturn(response);

        // when
        List<AircraftResponse> result = aircraftService.getAll();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findById: kayıt yoksa NotFoundException fırlatılır")
    void findById_whenNotFound_throwsNotFoundException() {
        // given
        when(aircraftRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> aircraftService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update: başarılı yolda günceller ve event yayınlar")
    void update_happyPath_updatesAndPublishes() {
        // given
        when(aircraftRepository.findById(1L)).thenReturn(Optional.of(aircraft));
        when(aircraftRepository.save(aircraft)).thenReturn(aircraft);
        when(aircraftMapper.toResponse(aircraft)).thenReturn(response);

        // when
        AircraftResponse result = aircraftService.update(1L, request);

        // then
        verify(aircraftMapper).updateEntity(request, aircraft);
        verify(eventPublisher).publish(eq(ReferenceEntityType.AIRCRAFT), eq(ChangeType.UPDATED), any(), any());
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("delete: başarılı yolda siler ve event yayınlar")
    void delete_happyPath_deletesAndPublishes() {
        // given
        when(aircraftRepository.findById(1L)).thenReturn(Optional.of(aircraft));

        // when
        aircraftService.delete(1L);

        // then
        verify(aircraftRepository).delete(aircraft);
        verify(eventPublisher).publish(ReferenceEntityType.AIRCRAFT, ChangeType.DELETED, "TC-JFA", null);
    }
}
