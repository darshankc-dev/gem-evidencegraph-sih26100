package com.gem.evidencegraph.demo;

import com.gem.evidencegraph.demo.dto.DemoBidSummaryDto;
import com.gem.evidencegraph.demo.dto.DemoDatasetResponseDto;
import com.gem.evidencegraph.demo.dto.DemoResetResponseDto;
import com.gem.evidencegraph.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoDatasetController.class)
@Import(SecurityConfig.class)
class DemoDatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoDatasetService demoDatasetService;

    @Test
    @DisplayName("POST /api/demo/load returns 200 with loaded demo dataset summary")
    void shouldLoadDemoDatasetSuccessfully() throws Exception {
        UUID tenderId = UUID.randomUUID();
        UUID bidAId = UUID.randomUUID();

        DemoBidSummaryDto bidSummary = DemoBidSummaryDto.builder()
                .bidId(bidAId)
                .bidReference("GEM-DEMO-BID-001")
                .bidderName("Apex Secure Systems Pvt Ltd")
                .scenarioName("Scenario A: Consistent / Low-Risk")
                .complianceStatus("COMPLIANT")
                .entityResolutionStatus("MATCH")
                .temporalStatus("VALID_AT_BID_DATE")
                .overallRiskLevel("LOW")
                .decisionRecommendation("RECOMMEND")
                .build();

        DemoDatasetResponseDto responseDto = DemoDatasetResponseDto.builder()
                .status("LOADED")
                .message("Synthetic demonstration dataset successfully loaded and evaluated.")
                .datasetLabel(DemoDatasetService.DATASET_LABEL)
                .tenderId(tenderId)
                .tenderReference("GEM-DEMO-2026-001")
                .tenderTitle("Supply of Network Security Equipment and Services")
                .bids(List.of(bidSummary))
                .totalBidders(3)
                .totalBids(3)
                .totalDocuments(15)
                .totalClaims(15)
                .totalEvidences(15)
                .totalRequirements(5)
                .build();

        when(demoDatasetService.loadDemoDataset()).thenReturn(responseDto);

        mockMvc.perform(post("/api/demo/load")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOADED"))
                .andExpect(jsonPath("$.tenderReference").value("GEM-DEMO-2026-001"))
                .andExpect(jsonPath("$.totalBids").value(3))
                .andExpect(jsonPath("$.bids[0].bidReference").value("GEM-DEMO-BID-001"))
                .andExpect(jsonPath("$.bids[0].decisionRecommendation").value("RECOMMEND"))
                .andExpect(jsonPath("$.datasetLabel").value(DemoDatasetService.DATASET_LABEL));
    }

    @Test
    @DisplayName("POST /api/demo/load returns ALREADY_LOADED when dataset already present")
    void shouldReturnAlreadyLoadedWhenDemoDatasetExists() throws Exception {
        DemoDatasetResponseDto responseDto = DemoDatasetResponseDto.builder()
                .status("ALREADY_LOADED")
                .message("Synthetic demonstration dataset is already loaded.")
                .datasetLabel(DemoDatasetService.DATASET_LABEL)
                .tenderReference("GEM-DEMO-2026-001")
                .totalBids(3)
                .build();

        when(demoDatasetService.loadDemoDataset()).thenReturn(responseDto);

        mockMvc.perform(post("/api/demo/load")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALREADY_LOADED"))
                .andExpect(jsonPath("$.tenderReference").value("GEM-DEMO-2026-001"));
    }

    @Test
    @DisplayName("POST /api/demo/reset returns 200 with reset counts")
    void shouldResetDemoDatasetSuccessfully() throws Exception {
        DemoResetResponseDto resetDto = DemoResetResponseDto.builder()
                .status("RESET_SUCCESSFUL")
                .message("Synthetic demonstration dataset was successfully removed.")
                .datasetLabel(DemoDatasetService.DATASET_LABEL)
                .deletedTenders(1)
                .deletedBidders(3)
                .deletedBids(3)
                .deletedDocuments(15)
                .deletedClaims(15)
                .deletedEvidence(15)
                .deletedRelationships(10)
                .build();

        when(demoDatasetService.resetDemoDataset()).thenReturn(resetDto);

        mockMvc.perform(post("/api/demo/reset")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESET_SUCCESSFUL"))
                .andExpect(jsonPath("$.deletedTenders").value(1))
                .andExpect(jsonPath("$.deletedBids").value(3))
                .andExpect(jsonPath("$.datasetLabel").value(DemoDatasetService.DATASET_LABEL));
    }

    @Test
    @DisplayName("GET /api/demo/status returns whether demo dataset is currently loaded")
    void shouldReturnDemoStatusSuccessfully() throws Exception {
        when(demoDatasetService.isDemoDatasetLoaded()).thenReturn(true);

        mockMvc.perform(get("/api/demo/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLoaded").value(true))
                .andExpect(jsonPath("$.datasetLabel").value(DemoDatasetService.DATASET_LABEL));
    }

}
