package com.gem.evidencegraph.dto;

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
public class DuplicateDocumentResponseDto {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String sha256Hash;
    private UUID existingDocumentId;
    private String path;

}
