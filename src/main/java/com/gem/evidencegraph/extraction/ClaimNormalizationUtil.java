package com.gem.evidencegraph.extraction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class ClaimNormalizationUtil {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
    );

    private ClaimNormalizationUtil() {

    }

    public static String normalize(String fieldName, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        if (fieldName == null) {
            return rawValue.trim();
        }

        return switch (fieldName.toUpperCase()) {
            case "GSTIN" -> normalizeGstin(rawValue);
            case "PAN" -> normalizePan(rawValue);
            case "UDYAM_NUMBER", "UDYAM" -> normalizeUdyam(rawValue);
            case "LEGAL_NAME" -> normalizeLegalName(rawValue);
            case "ADDRESS", "REGISTERED_ADDRESS" -> normalizeAddress(rawValue);
            case "REGISTRATION_DATE", "EXPIRY_DATE", "DATE" -> normalizeDate(rawValue);
            default -> normalizeGeneric(rawValue);
        };
    }

    public static String normalizeGstin(String gstin) {
        if (gstin == null) {
            return "";
        }
        return gstin.replaceAll("[\\s-]+", "").toUpperCase();
    }

    public static String normalizePan(String pan) {
        if (pan == null) {
            return "";
        }
        return pan.replaceAll("[\\s-]+", "").toUpperCase();
    }

    public static String normalizeUdyam(String udyam) {
        if (udyam == null) {
            return "";
        }
        String cleaned = udyam.replaceAll("\\s+", "-").replaceAll("-+", "-").toUpperCase().trim();
        if (cleaned.startsWith("-")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("-")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    public static String normalizeLegalName(String legalName) {
        if (legalName == null) {
            return "";
        }
        String cleaned = legalName.replaceAll("\\s+", " ").trim();

        cleaned = cleaned.replaceAll("[,.]+$", "").trim();
        return cleaned.toUpperCase();
    }

    public static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }

    public static String normalizeDate(String dateStr) {
        if (dateStr == null) {
            return "";
        }
        String trimmed = dateStr.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate parsed = LocalDate.parse(trimmed, formatter);
                return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {

            }
        }
        return trimmed.toUpperCase();
    }

    public static String normalizeGeneric(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim().toUpperCase();
    }

}
