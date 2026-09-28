package com.gem.evidencegraph.entityresolution;

import com.gem.evidencegraph.entityresolution.dto.EntityComparisonRequestDto;
import com.gem.evidencegraph.entityresolution.dto.EntityResolutionResultDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/entity-resolution")
@RequiredArgsConstructor
public class EntityResolutionController {

    private final EntityResolutionService entityResolutionService;

    @PostMapping("/compare")
    public ResponseEntity<EntityResolutionResultDto> compareProfiles(
            @Valid @RequestBody EntityComparisonRequestDto request) {
        log.info("Received entity resolution comparison request");
        EntityResolutionResultDto result = entityResolutionService.resolve(request.getLeft(), request.getRight());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bidders/{bidderId}/resolve")
    public ResponseEntity<EntityResolutionResultDto> resolveBidder(
            @PathVariable UUID bidderId) {
        log.info("Received entity resolution request for bidder ID: {}", bidderId);
        EntityResolutionResultDto result = entityResolutionService.resolveBidderAgainstEvidence(bidderId);
        return ResponseEntity.ok(result);
    }

}
