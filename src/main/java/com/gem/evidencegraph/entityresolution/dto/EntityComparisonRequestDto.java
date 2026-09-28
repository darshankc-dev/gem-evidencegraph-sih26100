package com.gem.evidencegraph.entityresolution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityComparisonRequestDto {

    @NotNull(message = "Left entity profile must not be null")
    private EntityProfileDto left;

    @NotNull(message = "Right entity profile must not be null")
    private EntityProfileDto right;

}
