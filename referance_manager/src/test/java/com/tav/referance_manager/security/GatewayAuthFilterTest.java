package com.tav.referance_manager.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

    private static final String SECRET = "rm-internal-secret";

    @Mock FilterChain filterChain;

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter();
        ReflectionTestUtils.setField(filter, "expectedGatewaySecret", SECRET);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Geçerli secret + headers → Authentication + chain")
    void validRequest_setsAuthAndProceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/airlines");
        request.addHeader("X-Gateway-Secret", SECRET);
        request.addHeader("X-User-Name", "alice");
        request.addHeader("X-User-Roles", "ROLE_OPERATION_OFFICER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(extractRoles(auth)).containsExactly("ROLE_OPERATION_OFFICER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Multi-role parse — virgülle ayrılmış")
    void multiRole_parsed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/airlines");
        request.addHeader("X-Gateway-Secret", SECRET);
        request.addHeader("X-User-Name", "alice");
        request.addHeader("X-User-Roles", "ROLE_OPERATION_OFFICER,ROLE_BI_SPECIALIST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(extractRoles(SecurityContextHolder.getContext().getAuthentication()))
                .containsExactlyInAnyOrder("ROLE_OPERATION_OFFICER", "ROLE_BI_SPECIALIST");
    }

    @Test
    @DisplayName("X-Gateway-Secret yok → 401")
    void missingSecret_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/airlines");
        request.addHeader("X-User-Name", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Yanlış secret → 401")
    void wrongSecret_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/airlines");
        request.addHeader("X-Gateway-Secret", "wrong");
        request.addHeader("X-User-Name", "alice");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Geçerli secret + X-User-Name yok → 401")
    void missingUserName_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/airlines");
        request.addHeader("X-Gateway-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Swagger/actuator → shouldNotFilter true")
    void swaggerAndActuator_skip() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/v3/api-docs"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health"))).isTrue();
    }

    @Test
    @DisplayName("Korumalı path → shouldNotFilter false")
    void protectedPath_appliesFilter() {
        assertThat(filter.shouldNotFilter(
                new MockHttpServletRequest("GET", "/api/reference/airlines"))).isFalse();
    }

    private Set<String> extractRoles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
