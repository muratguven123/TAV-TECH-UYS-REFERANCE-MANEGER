package com.tav.referance_manager.airline.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.airline.dto.AirlineRequest;
import com.tav.referance_manager.airline.dto.AirlineResponse;
import com.tav.referance_manager.airline.service.AirlineService;
import com.tav.referance_manager.common.exception.BusinessException;
import com.tav.referance_manager.common.exception.GlobalExceptionHandler;
import com.tav.referance_manager.common.exception.NotFoundException;
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
@WebMvcTest(controllers = AirlineController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AirlineControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AirlineService airlineService;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("GET /api/reference/airlines → 200 + liste döner")
    void list_returns200() throws Exception {
        when(airlineService.getAll()).thenReturn(List.of(new AirlineResponse(1L, "Turkish Airlines", "TK")));
        mockMvc.perform(get("/api/reference/airlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TK"));
    }

    @Test
    @DisplayName("GET /{id} → mevcut → 200")
    void getById_existing_returns200() throws Exception {
        when(airlineService.getById(1L)).thenReturn(new AirlineResponse(1L, "Turkish Airlines", "TK"));
        mockMvc.perform(get("/api/reference/airlines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /{id} → bulunamadı → 404")
    void getById_notFound_returns404() throws Exception {
        when(airlineService.getById(999L)).thenThrow(new NotFoundException("Havayolu bulunamadı"));
        mockMvc.perform(get("/api/reference/airlines/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST → geçerli payload → 201")
    void create_valid_returns201() throws Exception {
        AirlineRequest req = new AirlineRequest("Turkish Airlines", "TK");
        when(airlineService.create(any())).thenReturn(new AirlineResponse(1L, "Turkish Airlines", "TK"));
        mockMvc.perform(post("/api/reference/airlines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TK"));
    }

    @Test
    @DisplayName("POST → IATA kodu format hatası → 400")
    void create_invalidIataCode_returns400() throws Exception {
        AirlineRequest req = new AirlineRequest("Bad", "INVALID");
        mockMvc.perform(post("/api/reference/airlines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.code").exists());
    }

    @Test
    @DisplayName("POST → duplicate code (BusinessException) → 409")
    void create_duplicate_returns409() throws Exception {
        AirlineRequest req = new AirlineRequest("Turkish Airlines", "TK");
        when(airlineService.create(any())).thenThrow(new BusinessException("Bu IATA kodu zaten mevcut: TK"));
        mockMvc.perform(post("/api/reference/airlines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT → 200")
    void update_returns200() throws Exception {
        AirlineRequest req = new AirlineRequest("Turkish Airlines", "TK");
        when(airlineService.update(eq(1L), any())).thenReturn(new AirlineResponse(1L, "Turkish Airlines", "TK"));
        mockMvc.perform(put("/api/reference/airlines/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE → 204")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/reference/airlines/1"))
                .andExpect(status().isNoContent());
        verify(airlineService).delete(1L);
    }

    @Test
    @DisplayName("GET /by-code/{iata} → mevcut → 200")
    void findByCode_existing_returns200() throws Exception {
        when(airlineService.findByIataCode("TK"))
                .thenReturn(Optional.of(new AirlineResponse(1L, "Turkish Airlines", "TK")));
        mockMvc.perform(get("/api/reference/airlines/by-code/TK"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /by-code/{iata} → bulunamadı → 404")
    void findByCode_notFound_returns404() throws Exception {
        when(airlineService.findByIataCode("XX")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/reference/airlines/by-code/XX"))
                .andExpect(status().isNotFound());
    }
}
