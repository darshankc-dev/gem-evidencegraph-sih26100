package com.gem.evidencegraph.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoResetResponseDto {

    private String status;
    private String message;
    private String datasetLabel;
    private int deletedTenders;
    private int deletedBidders;
    private int deletedBids;
    private int deletedDocuments;
    private int deletedClaims;
    private int deletedEvidence;
    private int deletedRelationships;

}
