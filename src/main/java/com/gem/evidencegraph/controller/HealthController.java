package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.dto.HealthResponseDto;
import com.gem.evidencegraph.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        HealthResponseDto healthResponse = healthService.getHealthStatus();
        if ("UP".equalsIgnoreCase(healthResponse.getStatus())) {
            return ResponseEntity.ok(healthResponse);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthResponse);
    }

}
