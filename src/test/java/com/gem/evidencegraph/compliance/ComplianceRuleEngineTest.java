package com.gem.evidencegraph.compliance;

import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.compliance.rule.BidEvaluationContext;
import com.gem.evidencegraph.compliance.rule.ComplianceRuleEngine;
import com.gem.evidencegraph.compliance.rule.DeclarationRequirementRule;
import com.gem.evidencegraph.compliance.rule.DocumentRequirementRule;
import com.gem.evidencegraph.compliance.rule.EligibilityRequirementRule;
import com.gem.evidencegraph.compliance.rule.RegistrationRequirementRule;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceRuleEngineTest {

    private ComplianceRuleEngine ruleEngine;
    private Bid bid;
    private Bidder bidder;
    private Tender tender;

    @BeforeEach
    void setUp() {
        RegistrationRequirementRule regRule = new RegistrationRequirementRule();
        DocumentRequirementRule docRule = new DocumentRequirementRule();
        DeclarationRequirementRule declRule = new DeclarationRequirementRule();
        EligibilityRequirementRule eligRule = new EligibilityRequirementRule();

        ruleEngine = new ComplianceRuleEngine(List.of(regRule, docRule, declRule, eligRule));

        tender = Tender.builder().id(UUID.randomUUID()).tenderReference("GEM-TND-2026-001").build();
        bidder = Bidder.builder().id(UUID.randomUUID()).legalName("Apex Systems Pvt Ltd").build();
        bid = Bid.builder().id(UUID.randomUUID()).tender(tender).bidder(bidder).build();
    }

    @Test
    @DisplayName("Scenario 5: GST requirement + verified GST evidence -> COMPLIANT")
    void testScenario05_GstVerified_Compliant() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-GST")
                .name("GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.GST)
                .mandatory(true)
                .build();

        Claim claim = Claim.builder().fieldName("GSTIN").normalizedValue("29ABCDE1234F1Z5").build();
        Evidence ev = Evidence.builder()
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .value("29ABCDE1234F1Z5")
                .normalizedValue("29ABCDE1234F1Z5")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(ev))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.COMPLIANT);
        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
        assertThat(result.getCompliancePercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Scenario 6: GST requirement + mismatched GST evidence -> CONTRADICTORY")
    void testScenario06_GstMismatch_Contradictory() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-GST")
                .name("GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.GST)
                .mandatory(true)
                .build();

        Claim claim = Claim.builder().fieldName("GSTIN").normalizedValue("29ABCDE1234F1Z5").build();
        Evidence ev = Evidence.builder()
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .value("29ABCDE9999F1Z5")
                .verificationStatus(VerificationStatus.MISMATCH)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(ev))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.CONTRADICTORY);
        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
    }

    @Test
    @DisplayName("Scenario 7: GST requirement + unavailable source -> UNVERIFIED")
    void testScenario07_GstUnavailable_Unverified() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-GST")
                .name("GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.GST)
                .mandatory(true)
                .build();

        Claim claim = Claim.builder().fieldName("GSTIN").normalizedValue("29ABCDE1234F1Z5").build();
        Evidence ev = Evidence.builder()
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .value("29ABCDE1234F1Z5")
                .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(ev))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.UNVERIFIED);
        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
    }

    @Test
    @DisplayName("Scenario 8: GST requirement + no evidence/claim -> MISSING")
    void testScenario08_GstNoEvidence_Missing() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-GST")
                .name("GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.GST)
                .mandatory(true)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(Collections.emptyList())
                .evidences(Collections.emptyList())
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.MISSING);
        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
    }

    @Test
    @DisplayName("Scenario 9: PAN verified -> COMPLIANT")
    void testScenario09_PanVerified_Compliant() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-PAN")
                .name("PAN Requirement")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.PAN)
                .mandatory(true)
                .build();

        Claim claim = Claim.builder().fieldName("PAN").normalizedValue("ABCDE1234F").build();
        Evidence ev = Evidence.builder()
                .sourceType(SourceType.PAN)
                .attribute("PAN")
                .value("ABCDE1234F")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(ev))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
    }

    @Test
    @DisplayName("Scenario 10: PAN mismatch -> CONTRADICTORY")
    void testScenario10_PanMismatch_Contradictory() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-PAN")
                .name("PAN Requirement")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.PAN)
                .mandatory(true)
                .build();

        Claim claim = Claim.builder().fieldName("PAN").normalizedValue("ABCDE1234F").build();
        Evidence ev = Evidence.builder()
                .sourceType(SourceType.PAN)
                .attribute("PAN")
                .value("XYZAB9876K")
                .verificationStatus(VerificationStatus.MISMATCH)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(ev))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.CONTRADICTORY);
    }

    @Test
    @DisplayName("Scenario 11: Udyam verified -> COMPLIANT")
    void testScenario11_UdyamVerified_Compliant() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-UDYAM")
                .name("MSME Udyam Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.UDYAM)
                .mandatory(false)
                .build();

        Evidence ev = Evidence.builder()
                .sourceType(SourceType.UDYAM)
                .attribute("UDYAM_NUMBER")
                .value("UDYAM-KA-01-0000001")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(ev))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
    }

    @Test
    @DisplayName("Scenario 12: Udyam missing -> MISSING")
    void testScenario12_UdyamMissing_Missing() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-UDYAM")
                .name("MSME Udyam Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.UDYAM)
                .mandatory(false)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(Collections.emptyList())
                .evidences(Collections.emptyList())
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.MISSING);
    }

    @Test
    @DisplayName("Scenario 13: Required OEM document present and intact -> COMPLIANT")
    void testScenario13_OemDocumentPresent_Compliant() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-OEM")
                .name("OEM Authorization Certificate")
                .requirementType(RequirementType.DOCUMENT)
                .expectedDocumentType(DocumentType.OEM_AUTHORIZATION)
                .mandatory(true)
                .build();

        Document doc = Document.builder()
                .documentType(DocumentType.OEM_AUTHORIZATION)
                .originalFileName("OEM_Auth.pdf")
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .documents(List.of(doc))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
    }

    @Test
    @DisplayName("Scenario 14: Required OEM document absent -> MISSING")
    void testScenario14_OemDocumentAbsent_Missing() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-OEM")
                .name("OEM Authorization Certificate")
                .requirementType(RequirementType.DOCUMENT)
                .expectedDocumentType(DocumentType.OEM_AUTHORIZATION)
                .mandatory(true)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .documents(Collections.emptyList())
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.MISSING);
    }

    @Test
    @DisplayName("Scenario 15: Required declaration present -> COMPLIANT")
    void testScenario15_DeclarationPresent_Compliant() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-MII")
                .name("Make in India Declaration")
                .requirementType(RequirementType.DECLARATION)
                .expectedDocumentType(DocumentType.MAKE_IN_INDIA_DOCUMENT)
                .mandatory(false)
                .build();

        Document doc = Document.builder()
                .documentType(DocumentType.MAKE_IN_INDIA_DOCUMENT)
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .documents(List.of(doc))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
    }

    @Test
    @DisplayName("Scenario 16: Required declaration absent -> MISSING")
    void testScenario16_DeclarationAbsent_Missing() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-MII")
                .name("Make in India Declaration")
                .requirementType(RequirementType.DECLARATION)
                .expectedDocumentType(DocumentType.MAKE_IN_INDIA_DOCUMENT)
                .mandatory(true)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .documents(Collections.emptyList())
                .claims(Collections.emptyList())
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.MISSING);
    }

    @Test
    @DisplayName("Scenario 17: Multiple conflicting evidence records -> CONTRADICTORY")
    void testScenario17_ConflictingEvidence_Contradictory() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .requirementCode("REQ-GST")
                .name("GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .expectedSourceType(SourceType.GST)
                .mandatory(true)
                .build();

        Evidence ev1 = Evidence.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .value("29ABCDE1234F1Z5")
                .normalizedValue("29ABCDE1234F1Z5")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        Evidence ev2 = Evidence.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .value("29ABCDE9999F1Z5")
                .normalizedValue("29ABCDE9999F1Z5")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(ev1, ev2))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.CONTRADICTORY);
        assertThat(result.getRequirementResults().get(0).getContradictions()).isNotEmpty();
    }

    @Test
    @DisplayName("Scenario 18: All mandatory requirements compliant -> COMPLIANT")
    void testScenario18_AllMandatoryCompliant_OverallCompliant() {
        ComplianceRequirement req1 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-PAN").name("PAN")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.PAN).mandatory(true).build();
        ComplianceRequirement req2 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-GST").name("GST")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.GST).mandatory(true).build();

        Evidence panEv = Evidence.builder().sourceType(SourceType.PAN).attribute("PAN").value("ABCDE1234F").verificationStatus(VerificationStatus.VERIFIED).build();
        Evidence gstEv = Evidence.builder().sourceType(SourceType.GST).attribute("GSTIN").value("29ABCDE1234F1Z5").verificationStatus(VerificationStatus.VERIFIED).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(panEv, gstEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req1, req2));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.COMPLIANT);
        assertThat(result.getCompliancePercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Scenario 19: Mandatory requirement missing -> REVIEW")
    void testScenario19_MandatoryMissing_OverallReview() {
        ComplianceRequirement req1 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-PAN").name("PAN")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.PAN).mandatory(true).build();
        ComplianceRequirement req2 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-GST").name("GST")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.GST).mandatory(true).build();

        Evidence panEv = Evidence.builder().sourceType(SourceType.PAN).attribute("PAN").value("ABCDE1234F").verificationStatus(VerificationStatus.VERIFIED).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(panEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req1, req2));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
        assertThat(result.getMissingCount()).isEqualTo(1);
        assertThat(result.getCompliantCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 20: Mandatory requirement unverified -> REVIEW")
    void testScenario20_MandatoryUnverified_OverallReview() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-GST").name("GST")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.GST).mandatory(true).build();

        Claim claim = Claim.builder().fieldName("GSTIN").normalizedValue("29ABCDE1234F1Z5").build();
        Evidence gstEv = Evidence.builder().sourceType(SourceType.GST).attribute("GSTIN").value("29ABCDE1234F1Z5").verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(gstEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
        assertThat(result.getUnverifiedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 21: Mandatory requirement contradictory -> REVIEW")
    void testScenario21_MandatoryContradictory_OverallReview() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-PAN").name("PAN")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.PAN).mandatory(true).build();

        Claim claim = Claim.builder().fieldName("PAN").normalizedValue("ABCDE1234F").build();
        Evidence panEv = Evidence.builder().sourceType(SourceType.PAN).attribute("PAN").value("XYZAB9876K").verificationStatus(VerificationStatus.MISMATCH).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(panEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
        assertThat(result.getContradictoryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 22: Mandatory requirement clearly non-compliant -> NON_COMPLIANT")
    void testScenario22_MandatoryNonCompliant_OverallNonCompliant() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-EXP").name("Min Experience")
                .requirementType(RequirementType.ELIGIBILITY)
                .validationRule("minExperienceYears=5")
                .mandatory(true).build();

        Claim claim = Claim.builder().fieldName("EXPERIENCE_YEARS").normalizedValue("2").build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.NON_COMPLIANT);
        assertThat(result.getNonCompliantCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 23: Compliance percentage calculated correctly (3 of 4 -> 75.0%)")
    void testScenario23_PercentageCalculation() {
        ComplianceRequirement req1 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-1").name("Req 1")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.PAN).mandatory(true).build();
        ComplianceRequirement req2 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-2").name("Req 2")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.GST).mandatory(true).build();
        ComplianceRequirement req3 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-3").name("Req 3")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.UDYAM).mandatory(false).build();
        ComplianceRequirement req4 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-4").name("Req 4 (Missing)")
                .requirementType(RequirementType.DOCUMENT).expectedDocumentType(DocumentType.OEM_AUTHORIZATION).mandatory(false).build();

        Evidence panEv = Evidence.builder().sourceType(SourceType.PAN).attribute("PAN").value("ABCDE1234F").verificationStatus(VerificationStatus.VERIFIED).build();
        Evidence gstEv = Evidence.builder().sourceType(SourceType.GST).attribute("GSTIN").value("29ABCDE1234F1Z5").verificationStatus(VerificationStatus.VERIFIED).build();
        Evidence udyamEv = Evidence.builder().sourceType(SourceType.UDYAM).attribute("UDYAM_NUMBER").value("UDYAM-KA-01-0000001").verificationStatus(VerificationStatus.VERIFIED).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(panEv, gstEv, udyamEv))
                .documents(Collections.emptyList())
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req1, req2, req3, req4));

        assertThat(result.getCompliantCount()).isEqualTo(3);
        assertThat(result.getMissingCount()).isEqualTo(1);
        assertThat(result.getCompliancePercentage()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Scenario 24: Not-applicable requirements excluded from denominator")
    void testScenario24_NotApplicableExcludedFromDenominator() {
        ComplianceRequirement req1 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-1").name("PAN")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.PAN).mandatory(true).build();

        ComplianceRequirement req2 = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-2").name("Exempted")
                .requirementType(RequirementType.OTHER).mandatory(false).build();

        Evidence panEv = Evidence.builder().sourceType(SourceType.PAN).attribute("PAN").value("ABCDE1234F").verificationStatus(VerificationStatus.VERIFIED).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(panEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req1));

        assertThat(result.getCompliancePercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Scenario 25: Compliance engine never treats SOURCE_UNAVAILABLE as VERIFIED")
    void testScenario25_SourceUnavailableNeverTreatedAsVerified() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-GST").name("GST")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.GST).mandatory(true).build();

        Claim claim = Claim.builder().fieldName("GSTIN").normalizedValue("29ABCDE1234F1Z5").build();
        Evidence unavailEv = Evidence.builder()
                .sourceType(SourceType.GST).attribute("GSTIN").value("29ABCDE1234F1Z5")
                .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .claims(List.of(claim))
                .evidences(List.of(unavailEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getRequirementResults().get(0).getStatus()).isNotEqualTo(ComplianceStatus.COMPLIANT);
        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.UNVERIFIED);
    }

    @Test
    @DisplayName("Scenario 26: Compliance engine includes advisory summary and does not make final procurement decision")
    void testScenario26_AdvisoryAssessmentOnly() {
        ComplianceRequirement req = ComplianceRequirement.builder()
                .id(UUID.randomUUID()).requirementCode("REQ-PAN").name("PAN")
                .requirementType(RequirementType.REGISTRATION).expectedSourceType(SourceType.PAN).mandatory(true).build();

        Evidence panEv = Evidence.builder().sourceType(SourceType.PAN).attribute("PAN").value("ABCDE1234F").verificationStatus(VerificationStatus.VERIFIED).build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid).bidder(bidder).tender(tender)
                .evidences(List.of(panEv))
                .build();

        BidComplianceResultDto result = ruleEngine.evaluate(ctx, List.of(req));

        assertThat(result.getSummary()).contains("Advisory assessment only; final qualification decision rests with the procurement officer.");
    }

}
