package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.ComplianceRequirement;

public interface ComplianceRule {

    boolean supports(ComplianceRequirement requirement);

    ComplianceEvaluationResultDto evaluate(ComplianceRequirement requirement, BidEvaluationContext context);

}
