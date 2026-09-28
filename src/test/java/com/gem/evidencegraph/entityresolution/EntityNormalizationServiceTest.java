package com.gem.evidencegraph.entityresolution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNormalizationServiceTest {

    private EntityNormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new EntityNormalizationService();
    }

    @Test
    @DisplayName("Should normalize legal names with various Pvt Ltd suffixes to canonical form")
    void shouldNormalizeLegalNameSuffixesConsistently() {
        String expected = "ABC TECHNOLOGIES PRIVATE LIMITED";

        assertThat(normalizationService.normalizeLegalName("ABC Technologies Pvt. Ltd.")).isEqualTo(expected);
        assertThat(normalizationService.normalizeLegalName("ABC TECHNOLOGIES PRIVATE LIMITED")).isEqualTo(expected);
        assertThat(normalizationService.normalizeLegalName("ABC Technologies Private Ltd")).isEqualTo(expected);
        assertThat(normalizationService.normalizeLegalName("ABC Technologies Pvt Ltd")).isEqualTo(expected);
        assertThat(normalizationService.normalizeLegalName("ABC Technologies Pvt. Limited")).isEqualTo(expected);
        assertThat(normalizationService.normalizeLegalName("  abc   technologies   private   ltd.  ")).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should normalize Limited and LLP suffixes consistently")
    void shouldNormalizeOtherLegalSuffixes() {
        assertThat(normalizationService.normalizeLegalName("Tata Motors Ltd.")).isEqualTo("TATA MOTORS LIMITED");
        assertThat(normalizationService.normalizeLegalName("Tata Motors Limited")).isEqualTo("TATA MOTORS LIMITED");
        assertThat(normalizationService.normalizeLegalName("Alpha Consulting LLP")).isEqualTo("ALPHA CONSULTING LLP");
        assertThat(normalizationService.normalizeLegalName("Alpha Consulting Limited Liability Partnership")).isEqualTo("ALPHA CONSULTING LLP");
    }

    @Test
    @DisplayName("Should NOT over-normalize distinct business names to the same string")
    void shouldNotOverNormalizeDistinctBusinesses() {
        String name1 = normalizationService.normalizeLegalName("ABC Technologies");
        String name2 = normalizationService.normalizeLegalName("ABC Technology Solutions");

        assertThat(name1).isNotEqualTo(name2);
        assertThat(name1).isEqualTo("ABC TECHNOLOGIES");
        assertThat(name2).isEqualTo("ABC TECHNOLOGY SOLUTIONS");
    }

    @Test
    @DisplayName("Should normalize registered address with punctuation and abbreviations")
    void shouldNormalizeAddressDeterministically() {
        String addr1 = normalizationService.normalizeAddress("12, MG Road, Mysuru, Karnataka");
        String addr2 = normalizationService.normalizeAddress("12 MG ROAD MYSURU KARNATAKA");
        String addr3 = normalizationService.normalizeAddress("12, M.G. Rd, Mysuru, Karnataka");

        assertThat(addr1).isEqualTo("12 MG ROAD MYSURU KARNATAKA");
        assertThat(addr2).isEqualTo("12 MG ROAD MYSURU KARNATAKA");
        assertThat(addr3).isEqualTo("12 M G ROAD MYSURU KARNATAKA");

        String complexAddr = normalizationService.normalizeAddress("Flat No. 4B, 3rd Flr, Bldg 2, 5th St, Sect 4");
        assertThat(complexAddr).isEqualTo("FLAT NUMBER 4B 3RD FLOOR BUILDING 2 5TH STREET SECTOR 4");
    }

    @Test
    @DisplayName("Should normalize PAN numbers deterministically")
    void shouldNormalizePan() {
        assertThat(normalizationService.normalizePan("abcde1234f")).isEqualTo("ABCDE1234F");
        assertThat(normalizationService.normalizePan("ABCDE-1234-F")).isEqualTo("ABCDE1234F");
        assertThat(normalizationService.normalizePan("  ABCDE 1234 F  ")).isEqualTo("ABCDE1234F");
    }

    @Test
    @DisplayName("Should normalize GSTIN numbers deterministically")
    void shouldNormalizeGstin() {
        assertThat(normalizationService.normalizeGstin("29abcde1234f1z5")).isEqualTo("29ABCDE1234F1Z5");
        assertThat(normalizationService.normalizeGstin("29-ABCDE1234F-1Z5")).isEqualTo("29ABCDE1234F1Z5");
        assertThat(normalizationService.normalizeGstin(" 29 ABCDE1234F 1Z5 ")).isEqualTo("29ABCDE1234F1Z5");
    }

    @Test
    @DisplayName("Should normalize Udyam numbers deterministically")
    void shouldNormalizeUdyam() {
        assertThat(normalizationService.normalizeUdyamNumber("udyam-ka-01-0000001")).isEqualTo("UDYAM-KA-01-0000001");
        assertThat(normalizationService.normalizeUdyamNumber("UDYAM   KA  01  0000001")).isEqualTo("UDYAM-KA-01-0000001");
        assertThat(normalizationService.normalizeUdyamNumber(" -UDYAM-KA-01-0000001- ")).isEqualTo("UDYAM-KA-01-0000001");
    }

    @Test
    @DisplayName("Should normalize email addresses")
    void shouldNormalizeEmail() {
        assertThat(normalizationService.normalizeEmail("  Info@AbcTech.Com  ")).isEqualTo("info@abctech.com");
    }

    @Test
    @DisplayName("Should normalize phone numbers with country prefix safely")
    void shouldNormalizePhone() {
        assertThat(normalizationService.normalizePhone("+91 9876543210")).isEqualTo("9876543210");
        assertThat(normalizationService.normalizePhone("91-9876543210")).isEqualTo("9876543210");
        assertThat(normalizationService.normalizePhone("09876543210")).isEqualTo("9876543210");
        assertThat(normalizationService.normalizePhone("(987) 654-3210")).isEqualTo("9876543210");
    }

}
