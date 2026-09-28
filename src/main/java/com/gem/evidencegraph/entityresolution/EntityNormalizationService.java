package com.gem.evidencegraph.entityresolution;

import com.gem.evidencegraph.entityresolution.dto.EntityProfileDto;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class EntityNormalizationService {

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NON_ALPHANUMERIC_PAN_GST = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern ADDRESS_PUNCTUATION_PATTERN = Pattern.compile("[,.;:\\/\\\\#\\-_()'\"]");

    private static final Pattern PVT_LTD_PATTERN = Pattern.compile(
            "\\b(PVT\\.?\\s*LTD\\.?|PRIVATE\\s+LTD\\.?|PVT\\.?\\s*LIMITED|PRIVATE\\s+LIMITED)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LTD_PATTERN = Pattern.compile(
            "\\b(LTD\\.?|LIMITED)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LLP_PATTERN = Pattern.compile(
            "\\b(LLP|L\\.L\\.P\\.?|LIMITED\\s+LIABILITY\\s+PARTNERSHIP)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PROP_PATTERN = Pattern.compile(
            "\\b(PROP\\.?|PROPRIETORSHIP|PROPRIETARY)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INC_PATTERN = Pattern.compile(
            "\\b(INC\\.?|INCORPORATED)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CORP_PATTERN = Pattern.compile(
            "\\b(CORP\\.?|CORPORATION)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CO_PATTERN = Pattern.compile(
            "\\b(CO\\.?|COMPANY)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ROAD_PATTERN = Pattern.compile("\\b(RD)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STREET_PATTERN = Pattern.compile("\\b(ST)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(NO|NO\\.)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOOR_PATTERN = Pattern.compile("\\b(FLR)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILDING_PATTERN = Pattern.compile("\\b(BLDG)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern APARTMENT_PATTERN = Pattern.compile("\\b(APT)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTOR_PATTERN = Pattern.compile("\\b(SECT)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISTRICT_PATTERN = Pattern.compile("\\b(DIST)\\b", Pattern.CASE_INSENSITIVE);

    public EntityProfileDto normalizeProfile(EntityProfileDto profile) {
        if (profile == null) {
            return null;
        }
        return EntityProfileDto.builder()
                .legalName(normalizeLegalName(profile.getLegalName()))
                .pan(normalizePan(profile.getPan()))
                .gstin(normalizeGstin(profile.getGstin()))
                .udyamNumber(normalizeUdyamNumber(profile.getUdyamNumber()))
                .registeredAddress(normalizeAddress(profile.getRegisteredAddress()))
                .email(normalizeEmail(profile.getEmail()))
                .phone(normalizePhone(profile.getPhone()))
                .build();
    }

    public String normalizeLegalName(String legalName) {
        if (legalName == null || legalName.isBlank()) {
            return "";
        }

        String cleaned = legalName.trim();

        cleaned = cleaned.replace("\"", "").replace("'", "");

        cleaned = PVT_LTD_PATTERN.matcher(cleaned).replaceAll("PRIVATE LIMITED");
        cleaned = LLP_PATTERN.matcher(cleaned).replaceAll("LLP");
        cleaned = PROP_PATTERN.matcher(cleaned).replaceAll("PROPRIETORSHIP");
        cleaned = INC_PATTERN.matcher(cleaned).replaceAll("INCORPORATED");
        cleaned = CORP_PATTERN.matcher(cleaned).replaceAll("CORPORATION");

        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            boolean isPrevPrivate = (i > 0 && "PRIVATE".equalsIgnoreCase(words[i - 1]));
            if (!isPrevPrivate && (word.equalsIgnoreCase("LTD") || word.equalsIgnoreCase("LTD.") || word.equalsIgnoreCase("LIMITED"))) {
                sb.append("LIMITED");
            } else if (!isPrevPrivate && (word.equalsIgnoreCase("CO") || word.equalsIgnoreCase("CO.") || word.equalsIgnoreCase("COMPANY"))) {
                sb.append("COMPANY");
            } else {
                sb.append(word);
            }
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        cleaned = sb.toString();

        cleaned = cleaned.replaceAll("[.,\\-_/]", " ");
        cleaned = MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.toUpperCase();
    }

    public String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        String cleaned = address.replaceAll("[\\r\\n\\t]+", " ");
        cleaned = ADDRESS_PUNCTUATION_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();

        cleaned = ROAD_PATTERN.matcher(cleaned).replaceAll("ROAD");
        cleaned = STREET_PATTERN.matcher(cleaned).replaceAll("STREET");
        cleaned = NUMBER_PATTERN.matcher(cleaned).replaceAll("NUMBER");
        cleaned = FLOOR_PATTERN.matcher(cleaned).replaceAll("FLOOR");
        cleaned = BUILDING_PATTERN.matcher(cleaned).replaceAll("BUILDING");
        cleaned = APARTMENT_PATTERN.matcher(cleaned).replaceAll("APARTMENT");
        cleaned = SECTOR_PATTERN.matcher(cleaned).replaceAll("SECTOR");
        cleaned = DISTRICT_PATTERN.matcher(cleaned).replaceAll("DISTRICT");

        cleaned = MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.toUpperCase();
    }

    public String normalizePan(String pan) {
        if (pan == null || pan.isBlank()) {
            return "";
        }
        return NON_ALPHANUMERIC_PAN_GST.matcher(pan).replaceAll("").toUpperCase();
    }

    public String normalizeGstin(String gstin) {
        if (gstin == null || gstin.isBlank()) {
            return "";
        }
        return NON_ALPHANUMERIC_PAN_GST.matcher(gstin).replaceAll("").toUpperCase();
    }

    public String normalizeUdyamNumber(String udyam) {
        if (udyam == null || udyam.isBlank()) {
            return "";
        }
        String cleaned = udyam.replaceAll("\\s+", "-").replaceAll("-+", "-").trim().toUpperCase();
        if (cleaned.startsWith("-")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("-")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    public String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9+]", "").trim();
        if (digits.startsWith("+91") && digits.length() == 13) {
            digits = digits.substring(3);
        } else if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        return digits;
    }

}
