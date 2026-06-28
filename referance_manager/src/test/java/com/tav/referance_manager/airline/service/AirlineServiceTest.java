package com.tav.referance_manager.airline.service;

import com.tav.referance_manager.airline.domain.Airline;
import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import com.tav.referance_manager.airline.mapper.AirlineMapper;
import com.tav.referance_manager.airline.repository.AirlineRepository;
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
class AirlineServiceTest {

    @Mock AirlineRepository airlineRepository;
    @Mock AirlineMapper airlineMapper;
    @Mock ReferenceEventPublisher eventPublisher;

    @InjectMocks
    AirlineService airlineService;

    private Airline airline;
    private AirlineResponse response;
    private AirlineRequest request;

    @BeforeEach
    void setUp() {
        airline = Airline.builder().name("Turkish Airlines").code("TK").build();
        airline.setId(1L);
        response = new AirlineResponse(1L, "Turkish Airlines", "TK");
        request = new AirlineRequest("Turkish Airlines", "TK");
    }

    @Test
    @DisplayName("create: başarılı yolda kaydeder ve event yayınlar")
    void create_happyPath_savesAndPublishesEvent() {
        // given
        when(airlineRepository.existsByCode("TK")).thenReturn(false);
        when(airlineMapper.toEntity(request)).thenReturn(airline);
        when(airlineRepository.save(airline)).thenReturn(airline);
        when(airlineMapper.toResponse(airline)).thenReturn(response);

        // when
        AirlineResponse result = airlineService.create(request);

        // then
        assertThat(result).isEqualTo(response);
        verify(eventPublisher).publish(ReferenceEntityType.AIRLINE, ChangeType.CREATED, "TK", response);
    }

    @Test
    @DisplayName("create: tekrar eden kod varsa BusinessException fırlatılır")
    void create_whenDuplicateCode_throwsException() {
        // given
        when(airlineRepository.existsByCode("TK")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> airlineService.create(request))
                .isInstanceOf(BusinessException.class);
        verify(airlineRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll: tüm havayollarını döner")
    void findAll_returnsAllAirlines() {
        // given
        when(airlineRepository.findAll()).thenReturn(List.of(airline));
        when(airlineMapper.toResponse(airline)).thenReturn(response);

        // when
        List<AirlineResponse> result = airlineService.getAll();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findById: kayıt yoksa NotFoundException fırlatılır")
    void findById_whenNotFound_throwsNotFoundException() {
        // given
        when(airlineRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> airlineService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update: başarılı yolda günceller ve event yayınlar")
    void update_happyPath_updatesAndPublishes() {
        // given
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(airlineRepository.save(airline)).thenReturn(airline);
        when(airlineMapper.toResponse(airline)).thenReturn(response);

        // when
        AirlineResponse result = airlineService.update(1L, request);

        // then
        verify(airlineMapper).updateEntity(request, airline);
        verify(eventPublisher).publish(eq(ReferenceEntityType.AIRLINE), eq(ChangeType.UPDATED), any(), any());
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("delete: başarılı yolda siler ve event yayınlar")
    void delete_happyPath_deletesAndPublishes() {
        // given
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));

        // when
        airlineService.delete(1L);

        // then
        verify(airlineRepository).delete(airline);
        verify(eventPublisher).publish(ReferenceEntityType.AIRLINE, ChangeType.DELETED, "TK", null);
    }
}
