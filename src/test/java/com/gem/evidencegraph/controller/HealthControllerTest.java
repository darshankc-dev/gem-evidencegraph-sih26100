package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.dto.HealthResponseDto;
import com.gem.evidencegraph.security.SecurityConfig;
import com.gem.evidencegraph.service.HealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    @Test
    @DisplayName("GET /api/health should return UP status and application name")
    void shouldReturnHealthStatusSuccessfully() throws Exception {
        when(healthService.getHealthStatus()).thenReturn(
                HealthResponseDto.builder()
                        .status("UP")
                        .application("GeM EvidenceGraph")
                        .build()
        );

        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("GeM EvidenceGraph"));

        verify(healthService).getHealthStatus();
    }

    @Test
    @DisplayName("GET /api/health should return 503 SERVICE_UNAVAILABLE when status is DOWN")
    void shouldReturnServiceUnavailableWhenHealthStatusIsDown() throws Exception {
        when(healthService.getHealthStatus()).thenReturn(
                HealthResponseDto.builder()
                        .status("DOWN")
                        .application("GeM EvidenceGraph")
                        .build()
        );

        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.application").value("GeM EvidenceGraph"));

        verify(healthService).getHealthStatus();
    }

}

