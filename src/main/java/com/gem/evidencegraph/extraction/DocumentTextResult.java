package com.gem.evidencegraph.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTextResult {

    private int pageCount;
    private int totalCharacterCount;
    private String fullText;
    @Builder.Default
    private Map<Integer, String> pageTexts = Collections.emptyMap();
    private boolean hasTextLayer;

}
