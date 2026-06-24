package com.tav.referance_manager.station.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.GlobalExceptionHandler;
import com.tav.referance_manager.common.exception.NotFoundException;
import com.tav.referance_manager.station.dto.StationRequest;
import com.tav.referance_manager.station.dto.StationResponse;
import com.tav.referance_manager.station.service.StationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = StationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean StationService stationService;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("GET → liste 200")
    void list_returns200() throws Exception {
        when(stationService.getAll()).thenReturn(List.of(new StationResponse(1L, "LTFM", "Istanbul")));
        mockMvc.perform(get("/api/reference/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].icaoCode").value("LTFM"));
    }

    @Test
    @DisplayName("GET /{id} → mevcut 200")
    void getById_existing_returns200() throws Exception {
        when(stationService.getById(1L)).thenReturn(new StationResponse(1L, "LTFM", "Istanbul"));
        mockMvc.perform(get("/api/reference/stations/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} → bulunamadı 404")
    void getById_notFound_returns404() throws Exception {
        when(stationService.getById(999L)).thenThrow(new NotFoundException("İstasyon bulunamadı"));
        mockMvc.perform(get("/api/reference/stations/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST → geçerli 201")
    void create_valid_returns201() throws Exception {
        StationRequest req = new StationRequest("LTFM", "Istanbul Airport");
        when(stationService.create(any())).thenReturn(new StationResponse(1L, "LTFM", "Istanbul Airport"));
        mockMvc.perform(post("/api/reference/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST → geçersiz ICAO (küçük harf) → 400")
    void create_invalidIcao_returns400() throws Exception {
        StationRequest req = new StationRequest("ltfm", "Istanbul");
        mockMvc.perform(post("/api/reference/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.icaoCode").exists());
    }

    @Test
    @DisplayName("POST → duplicate ICAO → 409")
    void create_duplicate_returns409() throws Exception {
        StationRequest req = new StationRequest("LTFM", "Istanbul");
        when(stationService.create(any())).thenThrow(new BusinessException("Bu ICAO kodu zaten mevcut: LTFM"));
        mockMvc.perform(post("/api/reference/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT → 200")
    void update_returns200() throws Exception {
        StationRequest req = new StationRequest("LTFM", "Istanbul Airport");
        when(stationService.update(eq(1L), any()))
                .thenReturn(new StationResponse(1L, "LTFM", "Istanbul Airport"));
        mockMvc.perform(put("/api/reference/stations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE → 204")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/reference/stations/1"))
                .andExpect(status().isNoContent());
        verify(stationService).delete(1L);
    }

    @Test
    @DisplayName("GET /by-code/{icao} → mevcut 200")
    void findByIcao_existing_returns200() throws Exception {
        when(stationService.findByIcaoCode("LTFM"))
                .thenReturn(Optional.of(new StationResponse(1L, "LTFM", "Istanbul")));
        mockMvc.perform(get("/api/reference/stations/by-code/LTFM"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /by-code/{icao} → bulunamadı 404")
    void findByIcao_notFound_returns404() throws Exception {
        when(stationService.findByIcaoCode("XXXX")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/reference/stations/by-code/XXXX"))
                .andExpect(status().isNotFound());
    }
}
