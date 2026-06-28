package com.tav.referance_manager.route.service;

import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.uys.events.ChangeType;
import com.tav.uys.events.ReferenceEntityType;
import com.tav.referance_manager.events.ReferenceEventPublisher;
import com.tav.referance_manager.route.domain.Route;
import com.tav.referance_manager.route.dto.RouteRequest;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.route.mapper.RouteMapper;
import com.tav.referance_manager.route.repository.RouteRepository;
import com.tav.referance_manager.station.domain.Station;
import com.tav.referance_manager.station.dto.StationResponse;
import com.tav.referance_manager.station.service.StationService;
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
class RouteServiceTest {

    @Mock RouteRepository routeRepository;
    @Mock RouteMapper routeMapper;
    @Mock StationService stationService;
    @Mock ReferenceEventPublisher eventPublisher;

    @InjectMocks
    RouteService routeService;

    private Station origin;
    private Station destination;
    private Route route;
    private RouteResponse response;
    private RouteRequest request;

    @BeforeEach
    void setUp() {
        origin = Station.builder().icaoCode("LTFM").name("İstanbul").build();
        origin.setId(1L);
        destination = Station.builder().icaoCode("LTAC").name("Ankara").build();
        destination.setId(2L);

        route = Route.builder().originStation(origin).destinationStation(destination).build();
        route.setId(10L);

        StationResponse originResp = new StationResponse(1L, "LTFM", "İstanbul");
        StationResponse destResp   = new StationResponse(2L, "LTAC", "Ankara");
        response = new RouteResponse(10L, originResp, destResp);

        request = new RouteRequest(1L, 2L);
    }

    @Test
    @DisplayName("create: başarılı yolda kaydeder ve event yayınlar")
    void create_happyPath_savesAndPublishesEvent() {
        // given
        when(routeRepository.existsByOriginStationIdAndDestinationStationId(1L, 2L)).thenReturn(false);
        when(stationService.findEntityById(1L)).thenReturn(origin);
        when(stationService.findEntityById(2L)).thenReturn(destination);
        when(routeRepository.save(any(Route.class))).thenReturn(route);
        when(routeMapper.toResponse(route)).thenReturn(response);

        // when
        RouteResponse result = routeService.create(request);

        // then
        assertThat(result).isEqualTo(response);
        verify(routeRepository).save(any(Route.class));
        verify(eventPublisher).publish(eq(ReferenceEntityType.ROUTE), eq(ChangeType.CREATED), any(), any());
    }

    @Test
    @DisplayName("create: kalkış == varış ise BusinessException fırlatılır")
    void create_whenSameStation_throwsException() {
        // given
        RouteRequest sameStationReq = new RouteRequest(1L, 1L);

        // when / then
        assertThatThrownBy(() -> routeService.create(sameStationReq))
                .isInstanceOf(BusinessException.class);
        verify(routeRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll: tüm rotaları döner")
    void findAll_returnsAllRoutes() {
        // given
        when(routeRepository.findAll()).thenReturn(List.of(route));
        when(routeMapper.toResponse(route)).thenReturn(response);

        // when
        List<RouteResponse> result = routeService.getAll();

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findById: kayıt yoksa NotFoundException fırlatılır")
    void findById_whenNotFound_throwsNotFoundException() {
        // given
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> routeService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("delete: başarılı yolda siler ve event yayınlar")
    void delete_happyPath_deletesAndPublishes() {
        // given
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));

        // when
        routeService.delete(10L);

        // then
        verify(routeRepository).delete(route);
        verify(eventPublisher).publish(ReferenceEntityType.ROUTE, ChangeType.DELETED, "LTFM-LTAC", null);
    }
}
