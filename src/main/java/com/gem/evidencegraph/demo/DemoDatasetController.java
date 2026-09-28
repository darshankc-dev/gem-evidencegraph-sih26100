package com.gem.evidencegraph.demo;

import com.gem.evidencegraph.demo.dto.DemoDatasetResponseDto;
import com.gem.evidencegraph.demo.dto.DemoResetResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoDatasetController {

    private final DemoDatasetService demoDatasetService;

    @PostMapping("/load")
    public ResponseEntity<DemoDatasetResponseDto> loadDemoDataset() {
        log.info("Request received to load synthetic demonstration dataset");
        DemoDatasetResponseDto response = demoDatasetService.loadDemoDataset();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<DemoResetResponseDto> resetDemoDataset() {
        log.info("Request received to reset synthetic demonstration dataset");
        DemoResetResponseDto response = demoDatasetService.resetDemoDataset();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDemoStatus() {
        boolean isLoaded = demoDatasetService.isDemoDatasetLoaded();
        return ResponseEntity.ok(Map.of(
                "isLoaded", isLoaded,
                "datasetLabel", DemoDatasetService.DATASET_LABEL
        ));
    }

}
