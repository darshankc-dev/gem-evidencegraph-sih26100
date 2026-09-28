package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidOverviewDto {

    private UUID bidId;
    private String bidReference;
    private UUID bidderId;
    private String bidderLegalName;
    private UUID tenderId;
    private String tenderReference;
    private String tenderTitle;
    private LocalDateTime submissionDate;
    private BidStatus status;

    public static BidOverviewDto fromEntity(Bid bid) {
        if (bid == null) {
            return null;
        }
        return BidOverviewDto.builder()
                .bidId(bid.getId())
                .bidReference(bid.getBidReference())
                .bidderId(bid.getBidder() != null ? bid.getBidder().getId() : null)
                .bidderLegalName(bid.getBidder() != null ? bid.getBidder().getLegalName() : null)
                .tenderId(bid.getTender() != null ? bid.getTender().getId() : null)
                .tenderReference(bid.getTender() != null ? bid.getTender().getTenderReference() : null)
                .tenderTitle(bid.getTender() != null ? bid.getTender().getTitle() : null)
                .submissionDate(bid.getSubmissionDate())
                .status(bid.getStatus())
                .build();
    }

}
