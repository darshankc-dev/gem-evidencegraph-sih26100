package com.gem.evidencegraph.entityresolution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityProfileDto {

    private String legalName;
    private String pan;
    private String gstin;
    private String udyamNumber;
    private String registeredAddress;
    private String email;
    private String phone;

}
