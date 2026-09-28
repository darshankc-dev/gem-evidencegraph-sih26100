package com.gem.evidencegraph.demo;

import com.gem.evidencegraph.demo.dto.DemoDatasetResponseDto;
import com.gem.evidencegraph.demo.dto.DemoResetResponseDto;

public interface DemoDatasetService {

    String DATASET_LABEL = "SYNTHETIC DEMONSTRATION DATASET - For SIH26100 prototype/testing purposes only";

    DemoDatasetResponseDto loadDemoDataset();

    DemoResetResponseDto resetDemoDataset();

    boolean isDemoDatasetLoaded();

}
