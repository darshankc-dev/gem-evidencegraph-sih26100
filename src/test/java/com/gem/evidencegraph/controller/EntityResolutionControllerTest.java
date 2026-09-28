package com.gem.evidencegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gem.evidencegraph.config.GlobalExceptionHandler;
import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
import com.gem.evidencegraph.entityresolution.EntityResolutionController;
import com.gem.evidencegraph.entityresolution.EntityResolutionService;
import com.gem.evidencegraph.entityresolution.dto.EntityComparisonRequestDto;
import com.gem.evidencegraph.entityresolution.dto.EntityProfileDto;
import com.gem.evidencegraph.entityresolution.dto.EntityResolutionResultDto;
import com.gem.evidencegraph.security.SecurityConfig;
import com.gem.evidencegraph.util.ResourceNotFoundException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntityResolutionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EntityResolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EntityResolutionService entityResolutionService;

    @Test
    @DisplayName("POST /api/entity-resolution/compare should return 200 with MATCH result")
    void shouldCompareProfilesSuccessfully() throws Exception {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Pvt. Ltd.")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .udyamNumber("UDYAM-KA-01-0000001")
                .registeredAddress("12 MG Road Mysuru Karnataka")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC TECHNOLOGIES PRIVATE LIMITED")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .udyamNumber("UDYAM-KA-01-0000001")
                .registeredAddress("12, MG ROAD, MYSURU, KARNATAKA")
                .build();

        EntityComparisonRequestDto request = EntityComparisonRequestDto.builder()
                .left(left)
                .right(right)
                .build();

        EntityResolutionResultDto mockResult = EntityResolutionResultDto.builder()
                .matchStatus(EntityMatchStatus.MATCH)
                .confidence(1.0)
                .matchedAttributes(List.of("PAN", "GSTIN", "UDYAM_NUMBER", "LEGAL_NAME", "REGISTERED_ADDRESS"))
                .mismatchedAttributes(List.of())
                .missingAttributes(List.of())
                .explanation("Statutory identifier(s) match and normalized legal name is consistent.")
                .build();

        when(entityResolutionService.resolve(any(), any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/entity-resolution/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("MATCH"))
                .andExpect(jsonPath("$.confidence").value(1.0))
                .andExpect(jsonPath("$.matchedAttributes[0]").value("PAN"))
                .andExpect(jsonPath("$.explanation").value("Statutory identifier(s) match and normalized legal name is consistent."));
    }

    @Test
    @DisplayName("POST /api/entity-resolution/bidders/{bidderId}/resolve should return 200 with resolution")
    void shouldResolveBidderSuccessfully() throws Exception {
        UUID bidderId = UUID.randomUUID();

        EntityResolutionResultDto mockResult = EntityResolutionResultDto.builder()
                .matchStatus(EntityMatchStatus.MATCH)
                .confidence(1.0)
                .matchedAttributes(List.of("PAN", "GSTIN"))
                .mismatchedAttributes(List.of())
                .missingAttributes(List.of())
                .explanation("Verified statutory identifier(s) match.")
                .build();

        when(entityResolutionService.resolveBidderAgainstEvidence(eq(bidderId))).thenReturn(mockResult);

        mockMvc.perform(post("/api/entity-resolution/bidders/{bidderId}/resolve", bidderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("MATCH"))
                .andExpect(jsonPath("$.confidence").value(1.0));
    }

    @Test
    @DisplayName("POST /api/entity-resolution/bidders/{bidderId}/resolve should return 404 when bidder not found")
    void shouldReturn404WhenBidderNotFound() throws Exception {
        UUID bidderId = UUID.randomUUID();

        when(entityResolutionService.resolveBidderAgainstEvidence(eq(bidderId)))
                .thenThrow(new ResourceNotFoundException("Bidder with ID '" + bidderId + "' not found"));

        mockMvc.perform(post("/api/entity-resolution/bidders/{bidderId}/resolve", bidderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Bidder with ID '" + bidderId + "' not found"));
    }

}
