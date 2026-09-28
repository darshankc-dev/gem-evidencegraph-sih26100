package com.gem.evidencegraph.compliance.dto;

import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
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
public class RequirementResponseDto {

    private UUID id;
    private UUID tenderId;
    private String requirementCode;
    private String name;
    private String description;
    private RequirementType requirementType;
    private boolean mandatory;
    private SourceType expectedSourceType;
    private DocumentType expectedDocumentType;
    private String validationRule;
    private LocalDateTime createdAt;

}
