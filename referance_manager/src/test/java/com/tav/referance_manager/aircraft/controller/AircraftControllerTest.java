package com.tav.referance_manager.aircraft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.aircraft.dto.AircraftRequest;
import com.tav.referance_manager.aircraft.dto.AircraftResponse;
import com.tav.referance_manager.aircraft.service.AircraftService;
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
@WebMvcTest(controllers = AircraftController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AircraftControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AircraftService aircraftService;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("GET → liste 200")
    void list_returns200() throws Exception {
        when(aircraftService.getAll()).thenReturn(List.of(new AircraftResponse(1L, "A320", "TC-JFA")));
        mockMvc.perform(get("/api/reference/aircrafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tailNumber").value("TC-JFA"));
    }

    @Test
    @DisplayName("GET /{id} → mevcut 200")
    void getById_existing_returns200() throws Exception {
        when(aircraftService.getById(1L)).thenReturn(new AircraftResponse(1L, "A320", "TC-JFA"));
        mockMvc.perform(get("/api/reference/aircrafts/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} → bulunamadı 404")
    void getById_notFound_returns404() throws Exception {
        when(aircraftService.getById(999L)).thenThrow(new NotFoundException("Uçak bulunamadı"));
        mockMvc.perform(get("/api/reference/aircrafts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST → geçerli 201")
    void create_valid_returns201() throws Exception {
        AircraftRequest req = new AircraftRequest("A320", "TC-JFA");
        when(aircraftService.create(any())).thenReturn(new AircraftResponse(1L, "A320", "TC-JFA"));
        mockMvc.perform(post("/api/reference/aircrafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tailNumber").value("TC-JFA"));
    }

    @Test
    @DisplayName("POST → blank type 400")
    void create_blankType_returns400() throws Exception {
        AircraftRequest req = new AircraftRequest("", "TC-JFA");
        mockMvc.perform(post("/api/reference/aircrafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.type").exists());
    }

    @Test
    @DisplayName("POST → duplicate tail → 409")
    void create_duplicate_returns409() throws Exception {
        AircraftRequest req = new AircraftRequest("A320", "TC-JFA");
        when(aircraftService.create(any())).thenThrow(new BusinessException("Bu kuyruk numarası zaten mevcut: TC-JFA"));
        mockMvc.perform(post("/api/reference/aircrafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT → 200")
    void update_returns200() throws Exception {
        AircraftRequest req = new AircraftRequest("A321", "TC-JFA");
        when(aircraftService.update(eq(1L), any())).thenReturn(new AircraftResponse(1L, "A321", "TC-JFA"));
        mockMvc.perform(put("/api/reference/aircrafts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE → 204")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/reference/aircrafts/1"))
                .andExpect(status().isNoContent());
        verify(aircraftService).delete(1L);
    }

    @Test
    @DisplayName("GET /by-tail/{tail} → mevcut 200")
    void findByTail_existing_returns200() throws Exception {
        when(aircraftService.findByTailNumber("TC-JFA"))
                .thenReturn(Optional.of(new AircraftResponse(1L, "A320", "TC-JFA")));
        mockMvc.perform(get("/api/reference/aircrafts/by-tail/TC-JFA"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /by-tail/{tail} → bulunamadı 404")
    void findByTail_notFound_returns404() throws Exception {
        when(aircraftService.findByTailNumber("UNK")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/reference/aircrafts/by-tail/UNK"))
                .andExpect(status().isNotFound());
    }
}
