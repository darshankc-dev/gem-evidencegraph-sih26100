package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.dto.HealthResponseDto;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.BidRiskAssessmentRepository;
import com.gem.evidencegraph.repository.BidderRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.RiskAssessmentRepository;
import com.gem.evidencegraph.repository.RiskFindingRepository;
import com.gem.evidencegraph.repository.TenderRepository;
import com.gem.evidencegraph.service.HealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RiskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthService healthService;

    @Autowired
    private RiskService riskService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private TenderRepository tenderRepository;

    @Autowired
    private BidderRepository bidderRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private BidRiskAssessmentRepository bidRiskAssessmentRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private RiskFindingRepository riskFindingRepository;

    @Test
    @DisplayName("Runtime health check returns status UP and GeM EvidenceGraph")
    void testHealthCheck() throws Exception {
        HealthResponseDto health = healthService.getHealthStatus();
        assertEquals("UP", health.getStatus());
        assertEquals("GeM EvidenceGraph", health.getApplication());

        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("GeM EvidenceGraph"));
    }

    @Test
    @DisplayName("Verify Spring context injects all 3 new Step 9 repositories and RiskService")
    void testRepositoriesInjected() {
        assertNotNull(bidRiskAssessmentRepository);
        assertNotNull(riskAssessmentRepository);
        assertNotNull(riskFindingRepository);
        assertNotNull(riskService);
        assertNotNull(bidRepository);
        assertNotNull(tenderRepository);
        assertNotNull(bidderRepository);
        assertNotNull(documentRepository);
    }

}
