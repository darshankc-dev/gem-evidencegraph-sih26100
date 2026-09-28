package com.gem.evidencegraph.demo;

import com.gem.evidencegraph.compliance.ComplianceService;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.demo.dto.DemoBidSummaryDto;
import com.gem.evidencegraph.demo.dto.DemoDatasetResponseDto;
import com.gem.evidencegraph.demo.dto.DemoResetResponseDto;
import com.gem.evidencegraph.dto.DocumentResponseDto;
import com.gem.evidencegraph.dto.VerificationRequestDto;
import com.gem.evidencegraph.dto.VerificationResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidComplianceEvaluation;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.TemporalEvaluation;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.TenderStatus;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.entityresolution.EntityResolutionService;
import com.gem.evidencegraph.entityresolution.dto.EntityResolutionResultDto;
import com.gem.evidencegraph.extraction.DocumentExtractionPipelineService;
import com.gem.evidencegraph.repository.BidComplianceEvaluationRepository;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.BidRiskAssessmentRepository;
import com.gem.evidencegraph.repository.BidderRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.ComplianceRequirementRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.EntityResolutionResultRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.RiskAssessmentRepository;
import com.gem.evidencegraph.repository.RiskFindingRepository;
import com.gem.evidencegraph.repository.TemporalEvaluationRepository;
import com.gem.evidencegraph.repository.TenderRepository;
import com.gem.evidencegraph.risk.RiskService;
import com.gem.evidencegraph.risk.dto.BidRiskAssessmentResponseDto;
import com.gem.evidencegraph.service.DocumentIngestionService;
import com.gem.evidencegraph.service.DocumentStorageService;
import com.gem.evidencegraph.temporal.TemporalService;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.verification.adapter.MockDebarmentVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockGstVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockPanVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockUdyamVerificationAdapter;
import com.gem.evidencegraph.verification.gateway.VerificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDatasetServiceImpl implements DemoDatasetService {

    public static final String DEMO_TENDER_REF = "GEM-DEMO-2026-001";
    public static final String DEMO_BID_A_REF = "GEM-DEMO-BID-001";
    public static final String DEMO_BID_B_REF = "GEM-DEMO-BID-002";
    public static final String DEMO_BID_C_REF = "GEM-DEMO-BID-003";

    private final TenderRepository tenderRepository;
    private final BidderRepository bidderRepository;
    private final BidRepository bidRepository;
    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceRelationshipRepository evidenceRelationshipRepository;
    private final ComplianceRequirementRepository complianceRequirementRepository;
    private final BidComplianceEvaluationRepository bidComplianceEvaluationRepository;
    private final TemporalEvaluationRepository temporalEvaluationRepository;
    private final BidRiskAssessmentRepository bidRiskAssessmentRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskFindingRepository riskFindingRepository;
    private final EntityResolutionResultRepository entityResolutionResultRepository;

    private final DocumentIngestionService documentIngestionService;
    private final DocumentStorageService documentStorageService;
    private final DocumentExtractionPipelineService documentExtractionPipelineService;
    private final VerificationGateway verificationGateway;
    private final MockGstVerificationAdapter mockGstAdapter;
    private final MockPanVerificationAdapter mockPanAdapter;
    private final MockUdyamVerificationAdapter mockUdyamAdapter;
    private final MockDebarmentVerificationAdapter mockDebarmentAdapter;
    private final EntityResolutionService entityResolutionService;
    private final ComplianceService complianceService;
    private final TemporalService temporalService;
    private final RiskService riskService;

    @Override
    @Transactional(readOnly = true)
    public boolean isDemoDatasetLoaded() {
        return tenderRepository.findByTenderReference(DEMO_TENDER_REF).isPresent();
    }

    @Override
    @Transactional
    public DemoDatasetResponseDto loadDemoDataset() {
        log.info("Initiating synthetic demo dataset load for SIH26100 prototype demonstration");

        Optional<Tender> existingTender = tenderRepository.findByTenderReference(DEMO_TENDER_REF);
        if (existingTender.isPresent()) {
            log.info("Demo dataset is already loaded: {}", DEMO_TENDER_REF);
            Tender t = existingTender.get();
            List<Bid> existingBids = bidRepository.findByTenderId(t.getId());
            List<DemoBidSummaryDto> summaries = buildBidSummaries(existingBids);
            return DemoDatasetResponseDto.builder()
                    .status("ALREADY_LOADED")
                    .message("Synthetic demonstration dataset is already loaded.")
                    .datasetLabel(DATASET_LABEL)
                    .tenderId(t.getId())
                    .tenderReference(t.getTenderReference())
                    .tenderTitle(t.getTitle())
                    .bids(summaries)
                    .totalBidders(existingBids.size())
                    .totalBids(existingBids.size())
                    .totalDocuments((int) existingBids.stream().mapToLong(b -> documentRepository.findByBidId(b.getId()).size()).sum())
                    .totalClaims((int) existingBids.stream().mapToLong(b -> claimRepository.findByDocumentBidId(b.getId()).size()).sum())
                    .totalEvidences((int) existingBids.stream().mapToLong(b -> evidenceRepository.findByBidId(b.getId()).size()).sum())
                    .totalRequirements(complianceRequirementRepository.findByTenderId(t.getId()).size())
                    .build();
        }

        mockGstAdapter.clearMockResponses();
        mockDebarmentAdapter.clearMockResponses();
        mockPanAdapter.clearMockResponses();
        mockUdyamAdapter.clearMockResponses();

        Tender tender = Tender.builder()
                .id(UUID.randomUUID())
                .tenderReference(DEMO_TENDER_REF)
                .title("Supply of Network Security Equipment and Services")
                .description("SYNTHETIC DEMONSTRATION DATASET - For SIH26100 prototype/testing purposes only.")
                .publicationDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .submissionDeadline(LocalDateTime.of(2026, 8, 20, 17, 0))
                .status(TenderStatus.OPEN)
                .build();
        tender = tenderRepository.save(tender);
        log.info("Created synthetic tender: {} ({})", tender.getTitle(), tender.getTenderReference());

        createTenderRequirements(tender.getId());

        configureScenarioMockFixtures();

        List<DemoBidSummaryDto> summaries = new ArrayList<>();

        Bid bidA = createAndProcessBidA(tender);
        summaries.add(extractBidSummary(bidA, "Scenario A: Consistent / Low-Risk",
                "Clean statutory compliance, consistent identity across registries, verified evidence graph."));

        Bid bidB = createAndProcessBidB(tender);
        summaries.add(extractBidSummary(bidB, "Scenario B: Contradiction / Review",
                "Statutory registry identity conflict and conflicting certificate validity dates trigger officer review."));

        Bid bidC = createAndProcessBidC(tender);
        summaries.add(extractBidSummary(bidC, "Scenario C: Mandatory Non-Compliance / High-Risk",
                "Official external debarment registry confirmation and expired certificate trigger hard-fail non-compliance."));

        log.info("Synthetic demo dataset successfully created and processed through Steps 1-9 pipeline");

        int totalDocs = (int) bidRepository.findByTenderId(tender.getId()).stream()
                .mapToLong(b -> documentRepository.findByBidId(b.getId()).size()).sum();
        int totalClaims = (int) bidRepository.findByTenderId(tender.getId()).stream()
                .mapToLong(b -> claimRepository.findByDocumentBidId(b.getId()).size()).sum();
        int totalEvidences = (int) bidRepository.findByTenderId(tender.getId()).stream()
                .mapToLong(b -> evidenceRepository.findByBidId(b.getId()).size()).sum();
        int totalReqs = complianceRequirementRepository.findByTenderId(tender.getId()).size();

        return DemoDatasetResponseDto.builder()
                .status("LOADED")
                .message("Synthetic demonstration dataset successfully loaded and evaluated.")
                .datasetLabel(DATASET_LABEL)
                .tenderId(tender.getId())
                .tenderReference(tender.getTenderReference())
                .tenderTitle(tender.getTitle())
                .bids(summaries)
                .totalBidders(3)
                .totalBids(3)
                .totalDocuments(totalDocs)
                .totalClaims(totalClaims)
                .totalEvidences(totalEvidences)
                .totalRequirements(totalReqs)
                .build();
    }

    private void createTenderRequirements(UUID tenderId) {

        complianceService.createRequirement(tenderId, CreateRequirementRequestDto.builder()
                .requirementCode("REQ-GST")
                .name("GST Registration Verification")
                .description("Mandatory active GST registration verified against external GSTN registry.")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .build());

        complianceService.createRequirement(tenderId, CreateRequirementRequestDto.builder()
                .requirementCode("REQ-PAN")
                .name("PAN Identity Verification")
                .description("Mandatory corporate PAN verification against tax authority registry.")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.PAN)
                .build());

        complianceService.createRequirement(tenderId, CreateRequirementRequestDto.builder()
                .requirementCode("REQ-UDYAM")
                .name("Udyam/MSME Registration Verification")
                .description("Optional MSME registration certificate for priority preference.")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(false)
                .expectedSourceType(SourceType.UDYAM)
                .build());

        complianceService.createRequirement(tenderId, CreateRequirementRequestDto.builder()
                .requirementCode("REQ-OEM")
                .name("OEM Authorization Document")
                .description("Mandatory formal Manufacturer / OEM Authorization Form (MAF).")
                .requirementType(RequirementType.DOCUMENT)
                .mandatory(true)
                .expectedDocumentType(DocumentType.OEM_AUTHORIZATION)
                .build());

        complianceService.createRequirement(tenderId, CreateRequirementRequestDto.builder()
                .requirementCode("REQ-DECLARATION")
                .name("Non-Blacklisting / Debarment Declaration")
                .description("Mandatory formal undertaking declaring entity is not debarred or blacklisted.")
                .requirementType(RequirementType.DECLARATION)
                .mandatory(true)
                .expectedDocumentType(DocumentType.BLACKLIST_DECLARATION)
                .expectedSourceType(SourceType.DEBARMENT_LIST)
                .build());
    }

    private void configureScenarioMockFixtures() {

        mockGstAdapter.registerMockResponse("29BCDEF2345G1Z6", VerificationResultDto.builder()
                .sourceType(SourceType.GST)
                .sourceSystem(MockGstVerificationAdapter.SOURCE_SYSTEM)
                .adapterVersion(MockGstVerificationAdapter.ADAPTER_VERSION)
                .sourceReference("GSTN-REG-MISMATCH-002")
                .verificationStatus(VerificationStatus.MISMATCH)
                .subject("GSTIN")
                .attribute("GSTIN")
                .expectedValue("29BCDEF2345G1Z6")
                .observedValue("29BCDEF9999G1Z6")
                .normalizedObservedValue("29BCDEF9999G1Z6")
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .validFrom(LocalDateTime.of(2028, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2025, 1, 1, 0, 0))
                .rawResponseSnapshot("{\"gstin\": \"29BCDEF9999G1Z6\", \"status\": \"ACTIVE\", \"taxpayerType\": \"REGULAR\", \"note\": \"Registry record differs from claimed registration identity\"}")
                .reason("Observed GSTIN from registry conflicts with claimed value (synthetic contradiction scenario)")
                .build());

        mockDebarmentAdapter.registerMockResponse("CDEFG3456H", VerificationResultDto.builder()
                .sourceType(SourceType.DEBARMENT_LIST)
                .sourceSystem(MockDebarmentVerificationAdapter.SOURCE_SYSTEM)
                .adapterVersion(MockDebarmentVerificationAdapter.ADAPTER_VERSION)
                .sourceReference("CPPP-DEBAR-ORDER-2026-088")
                .verificationStatus(VerificationStatus.VERIFIED)
                .subject("PAN")
                .attribute("DEBARMENT_STATUS")
                .expectedValue("CDEFG3456H")
                .observedValue("DEBARRED")
                .normalizedObservedValue("DEBARRED")
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .validFrom(LocalDateTime.now().minusMonths(6))
                .validUntil(LocalDateTime.now().plusMonths(18))
                .rawResponseSnapshot("{\"identifier\": \"CDEFG3456H\", \"debarred\": true, \"order\": \"CPPP-DEBAR-2026-088\", \"reason\": \"Willful default on prior government contract\"}")
                .reason("External debarment registry confirms active blacklisting under CPPP Order 2026-088.")
                .build());

        mockGstAdapter.registerMockResponse("29CDEFG3456H1Z7", VerificationResultDto.builder()
                .sourceType(SourceType.GST)
                .sourceSystem(MockGstVerificationAdapter.SOURCE_SYSTEM)
                .adapterVersion(MockGstVerificationAdapter.ADAPTER_VERSION)
                .sourceReference("GSTN-REG-EXPIRED-003")
                .verificationStatus(VerificationStatus.VERIFIED)
                .subject("GSTIN")
                .attribute("GSTIN")
                .expectedValue("29CDEFG3456H1Z7")
                .observedValue("29CDEFG3456H1Z7")
                .normalizedObservedValue("29CDEFG3456H1Z7")
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .validFrom(LocalDateTime.of(2023, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2026, 5, 1, 0, 0))
                .rawResponseSnapshot("{\"gstin\": \"29CDEFG3456H1Z7\", \"status\": \"CANCELLED\", \"expiryDate\": \"2026-05-01\"}")
                .reason("Claim value matches source evidence, but certificate expired prior to bid submission.")
                .build());
    }

    private Bid createAndProcessBidA(Tender tender) {
        Bidder bidder = Bidder.builder()
                .id(UUID.randomUUID())
                .legalName("Apex Secure Systems Pvt Ltd")
                .normalizedName("APEX SECURE SYSTEMS PRIVATE LIMITED")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .udyamNumber("UDYAM-KA-00-0000001")
                .registeredAddress("12 Innovation Park, Bengaluru, Karnataka")
                .normalizedAddress("12 INNOVATION PARK BENGALURU KARNATAKA")
                .email("demo.apex@example.test")
                .phone("9000000001")
                .build();
        bidder = bidderRepository.save(bidder);

        Bid bid = Bid.builder()
                .id(UUID.randomUUID())
                .bidReference(DEMO_BID_A_REF)
                .submissionDate(LocalDateTime.of(2026, 8, 15, 14, 30))
                .status(BidStatus.SUBMITTED)
                .bidder(bidder)
                .tender(tender)
                .build();
        bid = bidRepository.save(bid);

        byte[] gstPdf = SyntheticPdfGenerator.generatePdf("GOODS AND SERVICES TAX REGISTRATION CERTIFICATE", List.of(
                "Legal Name: Apex Secure Systems Pvt Ltd",
                "GSTIN: 29ABCDE1234F1Z5",
                "PAN: ABCDE1234F",
                "Address: 12 Innovation Park, Bengaluru, Karnataka",
                "Registration Date: 10/04/2024",
                "Expiry Date: 10/04/2028",
                "Certificate No: GST-KA-2024-00123"
        ));
        ingestAndExtractDocument(bid, "Apex_GST_Certificate.pdf", gstPdf, DocumentType.GST_CERTIFICATE);

        byte[] panPdf = SyntheticPdfGenerator.generatePdf("INCOME TAX DEPARTMENT - PERMANENT ACCOUNT NUMBER", List.of(
                "Legal Name: Apex Secure Systems Pvt Ltd",
                "PAN: ABCDE1234F",
                "Registration Date: 15/01/2022",
                "Certificate No: PAN-DL-2022-9901"
        ));
        ingestAndExtractDocument(bid, "Apex_PAN_Document.pdf", panPdf, DocumentType.PAN_DOCUMENT);

        byte[] udyamPdf = SyntheticPdfGenerator.generatePdf("UDYAM REGISTRATION CERTIFICATE", List.of(
                "Legal Name: Apex Secure Systems Pvt Ltd",
                "UDYAM-KA-00-0000001",
                "Registration Date: 01/06/2023",
                "Certificate No: UDYAM-KA-2023-01"
        ));
        ingestAndExtractDocument(bid, "Apex_Udyam_Certificate.pdf", udyamPdf, DocumentType.UDYAM_CERTIFICATE);

        byte[] oemPdf = SyntheticPdfGenerator.generatePdf("MANUFACTURER AUTHORIZATION FORM (MAF)", List.of(
                "Legal Name: Apex Secure Systems Pvt Ltd",
                "Certificate No: OEM-AUTH-2026-APEX",
                "Registration Date: 01/01/2026",
                "Expiry Date: 31/12/2027",
                "Apex Secure Systems Pvt Ltd is authorized distributor and service partner."
        ));
        ingestAndExtractDocument(bid, "Apex_OEM_Authorization.pdf", oemPdf, DocumentType.OEM_AUTHORIZATION);

        byte[] declPdf = SyntheticPdfGenerator.generatePdf("UNDERTAKING / AFFIDAVIT OF NON-BLACKLISTING", List.of(
                "Legal Name: Apex Secure Systems Pvt Ltd",
                "PAN: ABCDE1234F",
                "Declaration Date: 10/08/2026",
                "We solemnly affirm that Apex Secure Systems Pvt Ltd has not been debarred or blacklisted."
        ));
        ingestAndExtractDocument(bid, "Apex_Blacklisting_Declaration.pdf", declPdf, DocumentType.BLACKLIST_DECLARATION);

        executePipeline(bid, bidder);
        return bid;
    }

    private Bid createAndProcessBidB(Tender tender) {
        Bidder bidder = Bidder.builder()
                .id(UUID.randomUUID())
                .legalName("BluePeak Digital Solutions Pvt Ltd")
                .normalizedName("BLUEPEAK DIGITAL SOLUTIONS PRIVATE LIMITED")
                .pan("BCDEF2345G")
                .gstin("29BCDEF2345G1Z6")
                .udyamNumber("UDYAM-KA-00-0000002")
                .registeredAddress("45 Cyber Gateway, Bengaluru, Karnataka")
                .normalizedAddress("45 CYBER GATEWAY BENGALURU KARNATAKA")
                .email("demo.bluepeak@example.test")
                .phone("9000000002")
                .build();
        bidder = bidderRepository.save(bidder);

        Bid bid = Bid.builder()
                .id(UUID.randomUUID())
                .bidReference(DEMO_BID_B_REF)
                .submissionDate(LocalDateTime.of(2026, 8, 16, 11, 0))
                .status(BidStatus.SUBMITTED)
                .bidder(bidder)
                .tender(tender)
                .build();
        bid = bidRepository.save(bid);

        byte[] gstPdf = SyntheticPdfGenerator.generatePdf("GOODS AND SERVICES TAX REGISTRATION CERTIFICATE", List.of(
                "Legal Name: BluePeak Digital Solutions Pvt Ltd",
                "GSTIN: 29BCDEF2345G1Z6",
                "PAN: BCDEF9999Z",
                "Address: 45 Cyber Gateway, Bengaluru, Karnataka",
                "Registration Date: 01/01/2028",
                "Expiry Date: 01/01/2025",
                "Certificate No: GST-KA-2024-00456"
        ));
        ingestAndExtractDocument(bid, "BluePeak_GST_Certificate.pdf", gstPdf, DocumentType.GST_CERTIFICATE);

        byte[] panPdf = SyntheticPdfGenerator.generatePdf("INCOME TAX DEPARTMENT - PERMANENT ACCOUNT NUMBER", List.of(
                "Legal Name: BluePeak Digital Solutions Pvt Ltd",
                "PAN: BCDEF2345G",
                "Registration Date: 10/05/2021",
                "Certificate No: PAN-DL-2021-4402"
        ));
        ingestAndExtractDocument(bid, "BluePeak_PAN_Document.pdf", panPdf, DocumentType.PAN_DOCUMENT);

        byte[] udyamPdf = SyntheticPdfGenerator.generatePdf("UDYAM REGISTRATION CERTIFICATE", List.of(
                "Legal Name: BluePeak Digital Solutions Pvt Ltd",
                "UDYAM-KA-00-0000002",
                "Registration Date: 15/02/2023",
                "Certificate No: UDYAM-KA-2023-02"
        ));
        ingestAndExtractDocument(bid, "BluePeak_Udyam_Certificate.pdf", udyamPdf, DocumentType.UDYAM_CERTIFICATE);

        byte[] oemPdf = SyntheticPdfGenerator.generatePdf("MANUFACTURER AUTHORIZATION FORM (MAF)", List.of(
                "Legal Name: BluePeak Digital Solutions Pvt Ltd",
                "Certificate No: OEM-AUTH-2026-BLUEPEAK",
                "Registration Date: 01/01/2026",
                "Expiry Date: 31/12/2027"
        ));
        ingestAndExtractDocument(bid, "BluePeak_OEM_Authorization.pdf", oemPdf, DocumentType.OEM_AUTHORIZATION);

        byte[] declPdf = SyntheticPdfGenerator.generatePdf("UNDERTAKING / AFFIDAVIT OF NON-BLACKLISTING", List.of(
                "Legal Name: BluePeak Digital Solutions Pvt Ltd",
                "PAN: BCDEF2345G",
                "Declaration Date: 11/08/2026",
                "We solemnly affirm that BluePeak Digital Solutions Pvt Ltd has not been debarred or blacklisted."
        ));
        ingestAndExtractDocument(bid, "BluePeak_Blacklisting_Declaration.pdf", declPdf, DocumentType.BLACKLIST_DECLARATION);

        executePipeline(bid, bidder);
        return bid;
    }

    private Bid createAndProcessBidC(Tender tender) {
        Bidder bidder = Bidder.builder()
                .id(UUID.randomUUID())
                .legalName("Crestline Infrastructure Systems Pvt Ltd")
                .normalizedName("CRESTLINE INFRASTRUCTURE SYSTEMS PRIVATE LIMITED")
                .pan("CDEFG3456H")
                .gstin("29CDEFG3456H1Z7")
                .udyamNumber("UDYAM-KA-00-0000003")
                .registeredAddress("78 Tech Corridor, Bengaluru, Karnataka")
                .normalizedAddress("78 TECH CORRIDOR BENGALURU KARNATAKA")
                .email("demo.crestline@example.test")
                .phone("9000000003")
                .build();
        bidder = bidderRepository.save(bidder);

        Bid bid = Bid.builder()
                .id(UUID.randomUUID())
                .bidReference(DEMO_BID_C_REF)
                .submissionDate(LocalDateTime.of(2026, 8, 17, 16, 0))
                .status(BidStatus.SUBMITTED)
                .bidder(bidder)
                .tender(tender)
                .build();
        bid = bidRepository.save(bid);

        byte[] gstPdf = SyntheticPdfGenerator.generatePdf("GOODS AND SERVICES TAX REGISTRATION CERTIFICATE", List.of(
                "Legal Name: Crestline Infrastructure Systems Pvt Ltd",
                "GSTIN: 29CDEFG3456H1Z7",
                "PAN: CDEFG3456H",
                "Address: 78 Tech Corridor, Bengaluru, Karnataka",
                "Registration Date: 01/01/2023",
                "Expiry Date: 01/05/2026",
                "Certificate No: GST-KA-2023-00999"
        ));
        ingestAndExtractDocument(bid, "Crestline_GST_Certificate.pdf", gstPdf, DocumentType.GST_CERTIFICATE);

        byte[] panPdf = SyntheticPdfGenerator.generatePdf("INCOME TAX DEPARTMENT - PERMANENT ACCOUNT NUMBER", List.of(
                "Legal Name: Crestline Infrastructure Systems Pvt Ltd",
                "PAN: CDEFG3456H",
                "Registration Date: 20/03/2020",
                "Certificate No: PAN-DL-2020-1192"
        ));
        ingestAndExtractDocument(bid, "Crestline_PAN_Document.pdf", panPdf, DocumentType.PAN_DOCUMENT);

        byte[] udyamPdf = SyntheticPdfGenerator.generatePdf("UDYAM REGISTRATION CERTIFICATE", List.of(
                "Legal Name: Crestline Infrastructure Systems Pvt Ltd",
                "UDYAM-KA-00-0000003",
                "Registration Date: 01/08/2023",
                "Certificate No: UDYAM-KA-2023-03"
        ));
        ingestAndExtractDocument(bid, "Crestline_Udyam_Certificate.pdf", udyamPdf, DocumentType.UDYAM_CERTIFICATE);

        byte[] oemPdf = SyntheticPdfGenerator.generatePdf("MANUFACTURER AUTHORIZATION FORM (MAF)", List.of(
                "Legal Name: Crestline Infrastructure Systems Pvt Ltd",
                "Certificate No: OEM-AUTH-2026-CRESTLINE",
                "Registration Date: 01/01/2026",
                "Expiry Date: 31/12/2027"
        ));
        ingestAndExtractDocument(bid, "Crestline_OEM_Authorization.pdf", oemPdf, DocumentType.OEM_AUTHORIZATION);

        byte[] declPdf = SyntheticPdfGenerator.generatePdf("UNDERTAKING / AFFIDAVIT OF NON-BLACKLISTING", List.of(
                "Legal Name: Crestline Infrastructure Systems Pvt Ltd",
                "PAN: CDEFG3456H",
                "Declaration Date: 12/08/2026",
                "We affirm that Crestline Infrastructure Systems Pvt Ltd has not been blacklisted."
        ));
        ingestAndExtractDocument(bid, "Crestline_Blacklisting_Declaration.pdf", declPdf, DocumentType.BLACKLIST_DECLARATION);

        executePipeline(bid, bidder);
        return bid;
    }

    private void ingestAndExtractDocument(Bid bid, String filename, byte[] content, DocumentType docType) {
        InMemoryMultipartFile file = new InMemoryMultipartFile("file", filename, "application/pdf", content);
        DocumentResponseDto docDto = documentIngestionService.uploadDocument(bid.getId(), file);

        Document doc = documentRepository.findById(docDto.getDocumentId()).orElseThrow();
        doc.setDocumentType(docType);
        doc.setIntegrityStatus(IntegrityStatus.VALID);
        documentRepository.save(doc);

        documentExtractionPipelineService.executeExtraction(doc.getId());
    }

    private void executePipeline(Bid bid, Bidder bidder) {

        verificationGateway.verifyBidClaims(bid.getId(), null);

        List<Claim> panClaims = claimRepository.findByDocumentBidId(bid.getId()).stream()
                .filter(c -> "PAN".equalsIgnoreCase(c.getFieldName()))
                .toList();

        for (Claim panClaim : panClaims) {
            verificationGateway.verifyClaim(VerificationRequestDto.builder()
                    .claimId(panClaim.getId())
                    .bidId(bid.getId())
                    .requestedSourceType(SourceType.DEBARMENT_LIST)
                    .build());
        }

        entityResolutionService.resolveBidderAgainstEvidence(bidder.getId());

        complianceService.evaluateBidCompliance(bid.getId());

        temporalService.evaluateBidTemporal(bid.getId());

        riskService.evaluateBidRisk(bid.getId());
    }

    private DemoBidSummaryDto extractBidSummary(Bid bid, String scenarioName, String scenarioDesc) {
        String erStatus = "—";
        List<EntityResolutionResult> errList = entityResolutionResultRepository.findByBidderIdOrderByCreatedAtDesc(bid.getBidder().getId());
        if (!errList.isEmpty()) {
            erStatus = errList.get(0).getMatchStatus().name();
        }

        String complianceStatus = "—";
        try {
            BidComplianceResultDto compDto = complianceService.getLatestBidCompliance(bid.getId());
            if (compDto != null && compDto.getOverallStatus() != null) {
                complianceStatus = compDto.getOverallStatus().name();
            }
        } catch (Exception ignored) {
        }

        String temporalStatus = "—";
        try {
            BidTemporalEvaluationResultDto tempDto = temporalService.getBidTemporal(bid.getId());
            if (tempDto != null && tempDto.getResults() != null && !tempDto.getResults().isEmpty()) {
                temporalStatus = tempDto.getResults().get(0).getStatus().name();
            }
        } catch (Exception ignored) {
        }

        String riskLevel = "—";
        Integer riskScore = null;
        String decision = "—";
        try {
            BidRiskAssessmentResponseDto riskDto = riskService.getBidRisk(bid.getId());
            if (riskDto != null) {
                riskLevel = riskDto.getOverallRiskLevel() != null ? riskDto.getOverallRiskLevel().name() : "—";
                riskScore = riskDto.getOverallRiskScore();
                decision = riskDto.getDecisionRecommendation() != null ? riskDto.getDecisionRecommendation().name() : "—";
            }
        } catch (Exception ignored) {
        }

        return DemoBidSummaryDto.builder()
                .bidId(bid.getId())
                .bidReference(bid.getBidReference())
                .bidderId(bid.getBidder().getId())
                .bidderName(bid.getBidder().getLegalName())
                .scenarioName(scenarioName)
                .scenarioDescription(scenarioDesc)
                .entityResolutionStatus(erStatus)
                .complianceStatus(complianceStatus)
                .temporalStatus(temporalStatus)
                .overallRiskLevel(riskLevel)
                .overallRiskScore(riskScore)
                .decisionRecommendation(decision)
                .build();
    }

    private List<DemoBidSummaryDto> buildBidSummaries(List<Bid> bids) {
        List<DemoBidSummaryDto> result = new ArrayList<>();
        for (Bid b : bids) {
            String name = "Scenario " + b.getBidReference();
            if (DEMO_BID_A_REF.equals(b.getBidReference())) {
                name = "Scenario A: Consistent / Low-Risk";
            } else if (DEMO_BID_B_REF.equals(b.getBidReference())) {
                name = "Scenario B: Contradiction / Review";
            } else if (DEMO_BID_C_REF.equals(b.getBidReference())) {
                name = "Scenario C: Mandatory Non-Compliance / High-Risk";
            }
            result.add(extractBidSummary(b, name, ""));
        }
        return result;
    }

    @Override
    @Transactional
    public DemoResetResponseDto resetDemoDataset() {
        log.info("Resetting synthetic demo dataset. Removing all records prefixed with GEM-DEMO-");

        int deletedBidsCount = 0;
        int deletedDocsCount = 0;
        int deletedClaimsCount = 0;
        int deletedEvidenceCount = 0;
        int deletedRelationshipsCount = 0;
        int deletedBiddersCount = 0;
        int deletedTendersCount = 0;

        Optional<Tender> demoTenderOpt = tenderRepository.findByTenderReference(DEMO_TENDER_REF);
        List<Bid> demoBids = new ArrayList<>();
        if (demoTenderOpt.isPresent()) {
            demoBids.addAll(bidRepository.findByTenderId(demoTenderOpt.get().getId()));
        }

        for (String ref : List.of(DEMO_BID_A_REF, DEMO_BID_B_REF, DEMO_BID_C_REF)) {
            Optional<Bid> bOpt = bidRepository.findByBidReference(ref);
            if (bOpt.isPresent() && !demoBids.contains(bOpt.get())) {
                demoBids.add(bOpt.get());
            }
        }

        for (Bid bid : demoBids) {
            UUID bidId = bid.getId();

            riskFindingRepository.deleteByBidId(bidId);
            riskAssessmentRepository.deleteByBidId(bidId);
            bidRiskAssessmentRepository.deleteByBidId(bidId);

            temporalEvaluationRepository.deleteByBidId(bidId);

            List<BidComplianceEvaluation> compEvals = bidComplianceEvaluationRepository.findByBidIdOrderByCreatedAtDesc(bidId);
            bidComplianceEvaluationRepository.deleteAll(compEvals);

            List<Document> docs = documentRepository.findByBidId(bidId);
            for (Document doc : docs) {
                List<Claim> claims = claimRepository.findByDocumentId(doc.getId());
                for (Claim claim : claims) {
                    List<Evidence> evidences = evidenceRepository.findByClaimId(claim.getId());
                    for (Evidence ev : evidences) {
                        List<EvidenceRelationship> targetRels = evidenceRelationshipRepository.findByTargetEvidenceId(ev.getId());
                        evidenceRelationshipRepository.deleteAll(targetRels);
                        deletedRelationshipsCount += targetRels.size();

                        List<EvidenceRelationship> sourceRels = evidenceRelationshipRepository.findBySourceEvidenceId(ev.getId());
                        evidenceRelationshipRepository.deleteAll(sourceRels);
                        deletedRelationshipsCount += sourceRels.size();

                        List<TemporalEvaluation> teList = temporalEvaluationRepository.findByEvidenceId(ev.getId());
                        temporalEvaluationRepository.deleteAll(teList);

                        evidenceRepository.delete(ev);
                        deletedEvidenceCount++;
                    }

                    List<EvidenceRelationship> claimRels = evidenceRelationshipRepository.findBySourceClaimId(claim.getId());
                    evidenceRelationshipRepository.deleteAll(claimRels);
                    deletedRelationshipsCount += claimRels.size();

                    claimRepository.delete(claim);
                    deletedClaimsCount++;
                }

                if (doc.getStoragePath() != null) {
                    try {
                        documentStorageService.delete(doc.getStoragePath());
                    } catch (Exception e) {
                        log.debug("Could not delete stored file: {}", doc.getStoragePath());
                    }
                }
                documentRepository.delete(doc);
                deletedDocsCount++;
            }

            List<Evidence> directEvs = evidenceRepository.findByBidId(bidId);
            evidenceRepository.deleteAll(directEvs);
            deletedEvidenceCount += directEvs.size();

            Bidder bidder = bid.getBidder();
            bidRepository.delete(bid);
            deletedBidsCount++;

            if (bidder != null) {
                List<EntityResolutionResult> errs = entityResolutionResultRepository.findByBidderId(bidder.getId());
                entityResolutionResultRepository.deleteAll(errs);
                bidderRepository.delete(bidder);
                deletedBiddersCount++;
            }
        }

        if (demoTenderOpt.isPresent()) {
            Tender t = demoTenderOpt.get();
            List<ComplianceRequirement> reqs = complianceRequirementRepository.findByTenderId(t.getId());
            complianceRequirementRepository.deleteAll(reqs);
            tenderRepository.delete(t);
            deletedTendersCount++;
        }

        mockGstAdapter.clearMockResponses();
        mockDebarmentAdapter.clearMockResponses();
        mockPanAdapter.clearMockResponses();
        mockUdyamAdapter.clearMockResponses();

        log.info("Reset complete. Deleted {} bids, {} docs, {} claims, {} evidence records",
                deletedBidsCount, deletedDocsCount, deletedClaimsCount, deletedEvidenceCount);

        return DemoResetResponseDto.builder()
                .status("RESET_SUCCESSFUL")
                .message("Synthetic demonstration dataset was successfully removed.")
                .datasetLabel(DATASET_LABEL)
                .deletedTenders(deletedTendersCount)
                .deletedBidders(deletedBiddersCount)
                .deletedBids(deletedBidsCount)
                .deletedDocuments(deletedDocsCount)
                .deletedClaims(deletedClaimsCount)
                .deletedEvidence(deletedEvidenceCount)
                .deletedRelationships(deletedRelationshipsCount)
                .build();
    }

}
