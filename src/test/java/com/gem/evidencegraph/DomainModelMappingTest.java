package com.gem.evidencegraph;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.ExtractionMethod;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.TenderStatus;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.BidderRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.ComplianceRequirementRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.TenderRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DomainModelMappingTest {

    @Autowired
    private BidderRepository bidderRepository;

    @Autowired
    private TenderRepository tenderRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private EvidenceRelationshipRepository evidenceRelationshipRepository;

    @Autowired
    private ComplianceRequirementRepository complianceRequirementRepository;

    @Test
    @DisplayName("Verify all 8 repositories are successfully instantiated by Spring context")
    void repositoriesShouldBeInjected() {
        assertNotNull(bidderRepository);
        assertNotNull(tenderRepository);
        assertNotNull(bidRepository);
        assertNotNull(documentRepository);
        assertNotNull(claimRepository);
        assertNotNull(evidenceRepository);
        assertNotNull(evidenceRelationshipRepository);
        assertNotNull(complianceRequirementRepository);
    }

    @Test
    @DisplayName("Verify all 8 entities have @Entity, @Table, and UUID @Id")
    void entitiesShouldBeConfiguredCorrectly() {
        List<Class<?>> entityClasses = List.of(
                Bidder.class,
                Tender.class,
                Bid.class,
                Document.class,
                Claim.class,
                Evidence.class,
                EvidenceRelationship.class,
                ComplianceRequirement.class
        );

        for (Class<?> entityClass : entityClasses) {
            assertTrue(entityClass.isAnnotationPresent(Entity.class),
                    entityClass.getSimpleName() + " must be annotated with @Entity");
            assertTrue(entityClass.isAnnotationPresent(Table.class),
                    entityClass.getSimpleName() + " must be annotated with @Table");

            boolean hasUuidId = false;
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    assertEquals(UUID.class, field.getType(),
                            entityClass.getSimpleName() + " primary key must be java.util.UUID");
                    hasUuidId = true;
                    break;
                }
            }
            assertTrue(hasUuidId, entityClass.getSimpleName() + " must have an @Id field of type UUID");
        }
    }

    @Test
    @DisplayName("Verify all enums contain required constant values")
    void enumsShouldContainExpectedConstants() {
        assertEquals(6, TenderStatus.values().length);
        assertNotNull(TenderStatus.valueOf("OPEN"));

        assertEquals(6, BidStatus.values().length);
        assertNotNull(BidStatus.valueOf("SUBMITTED"));

        assertEquals(13, DocumentType.values().length);
        assertNotNull(DocumentType.valueOf("GST_CERTIFICATE"));
        assertNotNull(DocumentType.valueOf("OTHER"));

        assertEquals(6, ExtractionStatus.values().length);
        assertNotNull(ExtractionStatus.valueOf("NOT_STARTED"));

        assertEquals(5, IntegrityStatus.values().length);
        assertNotNull(IntegrityStatus.valueOf("VALID"));

        assertEquals(4, ExtractionMethod.values().length);
        assertNotNull(ExtractionMethod.valueOf("DETERMINISTIC"));

        assertEquals(5, EvidenceType.values().length);
        assertNotNull(EvidenceType.valueOf("REGISTRY_RECORD"));

        assertEquals(11, SourceType.values().length);
        assertNotNull(SourceType.valueOf("GST"));
        assertNotNull(SourceType.valueOf("OTHER"));

        assertEquals(6, VerificationStatus.values().length);
        assertNotNull(VerificationStatus.valueOf("VERIFIED"));
        assertNotNull(VerificationStatus.valueOf("MISMATCH"));
        assertNotNull(VerificationStatus.valueOf("SOURCE_UNAVAILABLE"));

        assertEquals(6, RelationshipType.values().length);
        assertNotNull(RelationshipType.valueOf("SUPPORTS"));
        assertNotNull(RelationshipType.valueOf("CONTRADICTS"));

        assertEquals(6, RequirementType.values().length);
        assertNotNull(RequirementType.valueOf("ELIGIBILITY"));
    }

    @Test
    @DisplayName("Verify Bidder entity builder and property assignment")
    void shouldConstructBidderWithValidFields() {
        UUID id = UUID.randomUUID();
        Bidder bidder = Bidder.builder()
                .id(id)
                .legalName("Alpha Infotech Private Limited")
                .normalizedName("ALPHA INFOTECH PVT LTD")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .udyamNumber("UDYAM-KR-03-0012345")
                .registeredAddress("100 Industrial Area, Bengaluru, Karnataka")
                .normalizedAddress("100 INDUSTRIAL AREA BENGALURU KARNATAKA 560001")
                .email("contact@alphainfotech.example.com")
                .phone("+91-9876543210")
                .build();

        assertEquals(id, bidder.getId());
        assertEquals("Alpha Infotech Private Limited", bidder.getLegalName());
        assertEquals("ABCDE1234F", bidder.getPan());
        assertEquals("29ABCDE1234F1Z5", bidder.getGstin());
    }

    @Test
    @DisplayName("Verify Evidence and EvidenceRelationship graph edge construction")
    void shouldConstructEvidenceGraphNodesAndEdges() {
        Evidence sourceEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .evidenceType(EvidenceType.PORTAL_RESPONSE)
                .sourceType(SourceType.GST)
                .sourceReference("GSTN-API-2026")
                .subject("29ABCDE1234F1Z5")
                .attribute("filingStatus")
                .value("ACTIVE")
                .normalizedValue("ACTIVE")
                .verificationStatus(VerificationStatus.VERIFIED)
                .confidence(1.0)
                .build();

        Evidence targetEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .evidenceType(EvidenceType.DOCUMENT)
                .sourceType(SourceType.BID_DOCUMENT)
                .sourceReference("DOC-001")
                .subject("GST Certificate")
                .attribute("gstin")
                .value("29ABCDE1234F1Z5")
                .normalizedValue("29ABCDE1234F1Z5")
                .verificationStatus(VerificationStatus.VERIFIED)
                .confidence(0.98)
                .build();

        EvidenceRelationship relationship = EvidenceRelationship.builder()
                .id(UUID.randomUUID())
                .sourceEvidence(sourceEvidence)
                .targetEvidence(targetEvidence)
                .relationshipType(RelationshipType.SUPPORTS)
                .reason("GST Portal response confirms active status matching submitted certificate")
                .confidence(0.99)
                .build();

        assertEquals(RelationshipType.SUPPORTS, relationship.getRelationshipType());
        assertEquals(sourceEvidence, relationship.getSourceEvidence());
        assertEquals(targetEvidence, relationship.getTargetEvidence());
    }

    @Test
    @DisplayName("Verify Claim to Evidence edge with provenance metadata")
    void shouldConstructClaimToEvidenceEdgeWithProvenance() {
        Claim claim = Claim.builder()
                .id(UUID.randomUUID())
                .fieldName("GSTIN")
                .fieldValue("29ABCDE1234F1Z5")
                .normalizedValue("29ABCDE1234F1Z5")
                .extractionMethod(ExtractionMethod.DETERMINISTIC)
                .confidence(0.95)
                .build();

        Evidence evidence = Evidence.builder()
                .id(UUID.randomUUID())
                .claim(claim)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.GST)
                .sourceSystem("GSTN-PORTAL-MOCK")
                .adapterVersion("mock-gst-v1.0")
                .verificationStatus(VerificationStatus.VERIFIED)
                .value("29ABCDE1234F1Z5")
                .normalizedValue("29ABCDE1234F1Z5")
                .provenanceHash("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")
                .rawResponseSnapshot("{\"status\":\"ACTIVE\"}")
                .confidence(1.0)
                .build();

        EvidenceRelationship relationship = EvidenceRelationship.builder()
                .id(UUID.randomUUID())
                .sourceClaim(claim)
                .targetEvidence(evidence)
                .relationshipType(RelationshipType.VERIFIED_BY)
                .reason("Claim value matches source evidence")
                .confidence(1.0)
                .build();

        assertEquals(claim, evidence.getClaim());
        assertEquals("GSTN-PORTAL-MOCK", evidence.getSourceSystem());
        assertEquals("mock-gst-v1.0", evidence.getAdapterVersion());
        assertEquals(claim, relationship.getSourceClaim());
        assertEquals(evidence, relationship.getTargetEvidence());
        assertEquals(RelationshipType.VERIFIED_BY, relationship.getRelationshipType());
    }

}
