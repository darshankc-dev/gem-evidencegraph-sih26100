package com.gem.evidencegraph.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
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
import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceEngineDemonstrationTest {

    private ComplianceRuleEngine engine;
    private ObjectMapper objectMapper;
    private Tender tender;
    private Bidder bidder;
    private Bid bid;

    @BeforeEach
    void setUp() {
        engine = new ComplianceRuleEngine(List.of(
                new RegistrationRequirementRule(),
                new DocumentRequirementRule(),
                new DeclarationRequirementRule(),
                new EligibilityRequirementRule()
        ));

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        tender = Tender.builder()
                .id(UUID.fromString("2f4e9a11-c9f2-4bd5-912a-4318c679a9b1"))
                .tenderReference("GEM/2026/B/890123")
                .title("High-Performance Server Infrastructure Procurement")
                .build();

        bidder = Bidder.builder()
                .id(UUID.fromString("d1c44e90-e51c-4392-b43d-045a2bc1d89e"))
                .legalName("ABC Technologies Private Limited")
                .build();

        bid = Bid.builder()
                .id(UUID.fromString("8b5f3a02-12a8-4229-873b-fba0e3e7f411"))
                .bidReference("GEM-BID-2026-999")
                .tender(tender)
                .bidder(bidder)
                .submissionDate(LocalDateTime.now())
                .build();
    }

    private void saveJsonToFile(String filename, Object data) {
        try {
            File dir = new File("target/scratch");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            objectMapper.writeValue(new File(dir, filename), data);
        } catch (IOException e) {
            System.err.println("Could not write scratch file: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Demonstration Scenario 1: All mandatory requirements satisfied -> COMPLIANT")
    void demonstrate_scenario1_allCompliant() {
        ComplianceRequirement reqGst = ComplianceRequirement.builder()
                .id(UUID.fromString("3a11b2c3-4455-6677-8899-aabbccddeeff"))
                .tender(tender)
                .requirementCode("REQ-GST")
                .name("Valid GST Registration")
                .description("Bidder must have an active and verified GSTIN")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .build();

        ComplianceRequirement reqPan = ComplianceRequirement.builder()
                .id(UUID.fromString("4b22c3d4-5566-7788-9900-bbccddeeff00"))
                .tender(tender)
                .requirementCode("REQ-PAN")
                .name("Permanent Account Number")
                .description("Bidder must have an active verified PAN")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.PAN)
                .build();

        ComplianceRequirement reqOem = ComplianceRequirement.builder()
                .id(UUID.fromString("5c33d4e5-6677-8899-0011-ccddeeff0011"))
                .tender(tender)
                .requirementCode("REQ-OEM")
                .name("OEM Authorization Certificate")
                .description("Mandatory authorization from the hardware OEM")
                .requirementType(RequirementType.DOCUMENT)
                .mandatory(true)
                .expectedDocumentType(DocumentType.OEM_AUTHORIZATION)
                .build();

        Claim claimGst = Claim.builder()
                .id(UUID.randomUUID())
                .fieldName("GSTIN")
                .fieldValue("29ABCDE1234F1Z5")
                .build();

        Evidence evGst = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .claim(claimGst)
                .sourceType(SourceType.GST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .evidenceType(EvidenceType.PORTAL_RESPONSE)
                .rawResponseSnapshot("{\"gstin\":\"29ABCDE1234F1Z5\",\"status\":\"Active\"}")
                .build();

        Claim claimPan = Claim.builder()
                .id(UUID.randomUUID())
                .fieldName("PAN")
                .fieldValue("ABCDE1234F")
                .build();

        Evidence evPan = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .claim(claimPan)
                .sourceType(SourceType.PAN)
                .verificationStatus(VerificationStatus.VERIFIED)
                .evidenceType(EvidenceType.PORTAL_RESPONSE)
                .rawResponseSnapshot("{\"pan\":\"ABCDE1234F\",\"status\":\"Valid\"}")
                .build();

        Document docOem = Document.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .documentType(DocumentType.OEM_AUTHORIZATION)
                .fileName("OEM_Auth_Server.pdf")
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid)
                .bidder(bidder)
                .tender(tender)
                .documents(List.of(docOem))
                .claims(List.of(claimGst, claimPan))
                .evidences(List.of(evGst, evPan))
                .build();

        BidComplianceResultDto result = engine.evaluate(ctx, List.of(reqGst, reqPan, reqOem));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.COMPLIANT);
        assertThat(result.getCompliancePercentage()).isEqualTo(100.0);
        assertThat(result.getCompliantCount()).isEqualTo(3);
        assertThat(result.getMissingCount()).isEqualTo(0);
        assertThat(result.getContradictoryCount()).isEqualTo(0);
        assertThat(result.getUnverifiedCount()).isEqualTo(0);

        saveJsonToFile("demo_all_compliant.json", result);
    }

    @Test
    @DisplayName("Demonstration Scenario 2: Mandatory requirement missing -> REVIEW")
    void demonstrate_scenario2_missingMandatory() {
        ComplianceRequirement reqGst = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .requirementCode("REQ-GST")
                .name("Valid GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .build();

        ComplianceRequirement reqOem = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .requirementCode("REQ-OEM")
                .name("OEM Authorization Certificate")
                .requirementType(RequirementType.DOCUMENT)
                .mandatory(true)
                .expectedDocumentType(DocumentType.OEM_AUTHORIZATION)
                .build();

        Evidence evGst = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .sourceType(SourceType.GST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid)
                .bidder(bidder)
                .tender(tender)
                .documents(Collections.emptyList())
                .claims(Collections.emptyList())
                .evidences(List.of(evGst))
                .build();

        BidComplianceResultDto result = engine.evaluate(ctx, List.of(reqGst, reqOem));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
        assertThat(result.getCompliancePercentage()).isEqualTo(50.0);
        assertThat(result.getCompliantCount()).isEqualTo(1);
        assertThat(result.getMissingCount()).isEqualTo(1);
        assertThat(result.getRequirementResults().get(1).getStatus()).isEqualTo(ComplianceStatus.MISSING);
        assertThat(result.getSummary()).contains("MISSING: 1 requirement(s) have absent evidence");

        saveJsonToFile("demo_missing_mandatory.json", result);
    }

    @Test
    @DisplayName("Demonstration Scenario 3: Contradictory evidence detected -> REVIEW")
    void demonstrate_scenario3_contradictoryEvidence() {
        ComplianceRequirement reqGst = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .requirementCode("REQ-GST")
                .name("Valid GST Registration")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .build();

        Evidence evGstMismatch = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .sourceType(SourceType.GST)
                .verificationStatus(VerificationStatus.MISMATCH)
                .evidenceType(EvidenceType.PORTAL_RESPONSE)
                .rawResponseSnapshot("{\"error\":\"Entity name does not match GST registration records\"}")
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid)
                .bidder(bidder)
                .tender(tender)
                .documents(Collections.emptyList())
                .claims(Collections.emptyList())
                .evidences(List.of(evGstMismatch))
                .build();

        BidComplianceResultDto result = engine.evaluate(ctx, List.of(reqGst));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
        assertThat(result.getContradictoryCount()).isEqualTo(1);
        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.CONTRADICTORY);
        assertThat(result.getRequirementResults().get(0).getReason()).contains("mismatch");

        saveJsonToFile("demo_contradictory_evidence.json", result);
    }

    @Test
    @DisplayName("Demonstration Scenario 4: External verification unverified (Source unavailable) -> REVIEW")
    void demonstrate_scenario4_unverifiedEvidence() {
        ComplianceRequirement reqUdyam = ComplianceRequirement.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .requirementCode("REQ-UDYAM")
                .name("Udyam MSME Registration")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.UDYAM)
                .build();

        Evidence evUdyamUnavailable = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .sourceType(SourceType.UDYAM)
                .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                .evidenceType(EvidenceType.PORTAL_RESPONSE)
                .rawResponseSnapshot("{\"error\":\"MSME Gateway connection timed out\"}")
                .build();

        BidEvaluationContext ctx = BidEvaluationContext.builder()
                .bid(bid)
                .bidder(bidder)
                .tender(tender)
                .documents(Collections.emptyList())
                .claims(Collections.emptyList())
                .evidences(List.of(evUdyamUnavailable))
                .build();

        BidComplianceResultDto result = engine.evaluate(ctx, List.of(reqUdyam));

        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.REVIEW);
        assertThat(result.getUnverifiedCount()).isEqualTo(1);
        assertThat(result.getRequirementResults().get(0).getStatus()).isEqualTo(ComplianceStatus.UNVERIFIED);
        assertThat(result.getRequirementResults().get(0).getReason()).contains("unavailable");

        saveJsonToFile("demo_unverified_evidence.json", result);
    }
}
