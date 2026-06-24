package com.tav.referance_manager.route.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.GlobalExceptionHandler;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.route.dto.RouteRequest;
import com.tav.referance_manager.route.dto.RouteResponse;
import com.tav.referance_manager.route.service.RouteService;
import com.tav.referance_manager.station.dto.StationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RouteControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RouteService routeService;

    private final ObjectMapper om = new ObjectMapper();

    private RouteResponse sampleResponse() {
        return new RouteResponse(1L,
                new StationResponse(10L, "LTFM", "Istanbul"),
                new StationResponse(20L, "LTAC", "Ankara"));
    }

    @Test
    @DisplayName("GET → liste 200")
    void list_returns200() throws Exception {
        when(routeService.getAll()).thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/api/reference/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originStation.icaoCode").value("LTFM"));
    }

    @Test
    @DisplayName("GET /{id} → mevcut 200")
    void getById_existing_returns200() throws Exception {
        when(routeService.getById(1L)).thenReturn(sampleResponse());
        mockMvc.perform(get("/api/reference/routes/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} → bulunamadı 404")
    void getById_notFound_returns404() throws Exception {
        when(routeService.getById(999L)).thenThrow(new NotFoundException("Rota bulunamadı"));
        mockMvc.perform(get("/api/reference/routes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST → geçerli 201")
    void create_valid_returns201() throws Exception {
        RouteRequest req = new RouteRequest(10L, 20L);
        when(routeService.create(any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/api/reference/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST → originStationId == destinationStationId → 409 (BusinessException)")
    void create_sameOriginAndDestination_returns409() throws Exception {
        RouteRequest req = new RouteRequest(10L, 10L);
        when(routeService.create(any())).thenThrow(new BusinessException("Kalkış ve varış istasyonu aynı olamaz"));
        mockMvc.perform(post("/api/reference/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST → originStationId null → 400")
    void create_nullOrigin_returns400() throws Exception {
        RouteRequest req = new RouteRequest(null, 20L);
        mockMvc.perform(post("/api/reference/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.originStationId").exists());
    }

    @Test
    @DisplayName("POST → duplicate route → 409")
    void create_duplicate_returns409() throws Exception {
        RouteRequest req = new RouteRequest(10L, 20L);
        when(routeService.create(any())).thenThrow(new BusinessException("Bu rota zaten mevcut"));
        mockMvc.perform(post("/api/reference/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE → 204")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/reference/routes/1"))
                .andExpect(status().isNoContent());
        verify(routeService).delete(1L);
    }

    @Test
    @DisplayName("GET /by-codes → mevcut 200")
    void findByCodes_existing_returns200() throws Exception {
        when(routeService.findByCodes("LTFM", "LTAC")).thenReturn(Optional.of(sampleResponse()));
        mockMvc.perform(get("/api/reference/routes/by-codes")
                        .param("origin", "LTFM")
                        .param("destination", "LTAC"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /by-codes → bulunamadı 404")
    void findByCodes_notFound_returns404() throws Exception {
        when(routeService.findByCodes("LTFM", "LTAC")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/reference/routes/by-codes")
                        .param("origin", "LTFM")
                        .param("destination", "LTAC"))
                .andExpect(status().isNotFound());
    }
}
