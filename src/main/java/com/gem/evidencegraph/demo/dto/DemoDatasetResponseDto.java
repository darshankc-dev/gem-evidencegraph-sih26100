package com.gem.evidencegraph.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoDatasetResponseDto {

    private String status;
    private String message;
    private String datasetLabel;
    private UUID tenderId;
    private String tenderReference;
    private String tenderTitle;
    private List<DemoBidSummaryDto> bids;
    private int totalBidders;
    private int totalBids;
    private int totalDocuments;
    private int totalClaims;
    private int totalEvidences;
    private int totalRequirements;

}
