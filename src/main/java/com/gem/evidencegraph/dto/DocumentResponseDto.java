package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.entity.IntegrityStatus;
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
public class DocumentResponseDto {

    private UUID documentId;
    private UUID bidId;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String sha256Hash;
    private DocumentType documentType;
    private Integer version;
    private ExtractionStatus extractionStatus;
    private IntegrityStatus integrityStatus;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;

    public static DocumentResponseDto fromEntity(Document document) {
        if (document == null) {
            return null;
        }
        return DocumentResponseDto.builder()
                .documentId(document.getId())
                .bidId(document.getBid() != null ? document.getBid().getId() : null)
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .sha256Hash(document.getSha256Hash())
                .documentType(document.getDocumentType())
                .version(document.getVersion())
                .extractionStatus(document.getExtractionStatus())
                .integrityStatus(document.getIntegrityStatus())
                .uploadedAt(document.getUploadedAt())
                .createdAt(document.getCreatedAt())
                .build();
    }

}
