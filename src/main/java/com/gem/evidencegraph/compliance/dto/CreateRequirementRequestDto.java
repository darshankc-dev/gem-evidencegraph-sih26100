package com.gem.evidencegraph.compliance.dto;

import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequirementRequestDto {

    @NotBlank(message = "Requirement code must not be blank")
    private String requirementCode;

    @NotBlank(message = "Requirement name must not be blank")
    private String name;

    private String description;

    @NotNull(message = "Requirement type must not be null")
    private RequirementType requirementType;

    @Builder.Default
    private boolean mandatory = true;

    private SourceType expectedSourceType;

    private DocumentType expectedDocumentType;

    private String validationRule;

}
