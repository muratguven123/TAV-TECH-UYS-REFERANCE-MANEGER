package com.tav.referance_manager.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tav.referance_manager.auth.dto.LoginRequest;
import com.tav.referance_manager.auth.dto.RegisterRequest;
import com.tav.referance_manager.user.domain.Role;
import com.tav.referance_manager.user.domain.RoleName;
import com.tav.referance_manager.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:authtest;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.bootstrap-servers=",
        "app.jwt.secret=test-secret-key-minimum-32-bytes-ok!",
        "app.jwt.expiration-ms=3600000"
})
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName(RoleName.OPERATION_OFFICER).isEmpty()) {
            Role r = new Role();
            r.setName(RoleName.OPERATION_OFFICER);
            roleRepository.save(r);
        }
        if (roleRepository.findByName(RoleName.BI_SPECIALIST).isEmpty()) {
            Role r = new Role();
            r.setName(RoleName.BI_SPECIALIST);
            roleRepository.save(r);
        }
    }

    @Test
    void registerAndLogin_success() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "ali", "ali@tav.com", "Aa1!aaaa", Set.of("OPERATION_OFFICER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ali"))
                .andExpect(jsonPath("$.password").doesNotExist());

        LoginRequest login = new LoginRequest("ali", "Aa1!aaaa");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Protected endpoint without token → 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ali", "WrongPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "duplicate", "d1@tav.com", "Aa1!aaaa", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        RegisterRequest dup = new RegisterRequest(
                "duplicate", "d2@tav.com", "Aa1!aaaa", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "weak", "weak@tav.com", "password", null);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }
}
